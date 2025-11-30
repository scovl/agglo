defmodule Agglo.ObjectStorage do
  @moduledoc """
  Camada utilitária que simula um storage de objetos usando o
  sistema de arquivos local (ex.: `priv/object_storage`).

  Esta abordagem permite gerar materiais efêmeros (páginas de feed)
  sem manter o conteúdo em memória ou em um banco relacional.
  """

  @base_dir Application.compile_env(:agglo, :object_storage_dir,
            Path.join(:code.priv_dir(:agglo), "object_storage")
          )

  def base_dir, do: @base_dir

  @doc """
  Grava um mapa como JSON no caminho relativo informado.
  Cria diretórios necessários e retorna a chave (path relativo).
  """
  def write_json!(relative_path, payload) do
    full_path = to_full_path(relative_path)
    full_path |> Path.dirname() |> File.mkdir_p!()

    payload
    |> Jason.encode_to_iodata!()
    |> then(&File.write!(full_path, &1))

    relative_path
  end

  @doc """
  Lê um JSON do caminho relativo e retorna `{:ok, map}` ou erro.
  """
  def read_json(relative_path) do
    with {:ok, content} <- File.read(to_full_path(relative_path)),
         {:ok, decoded} <- Jason.decode(content) do
      {:ok, decoded}
    end
  end

  @doc """
  Remove arquivos antigos dentro de um prefixo, mantendo `keep` mais
  recentes (com base em `mtime`). Arquivos especiais como `latest.json`
  são preservados.
  """
  def cleanup_prefix!(prefix, keep: keep) do
    dir = Path.join(@base_dir, prefix)

    if File.dir?(dir) do
      dir
      |> File.ls!()
      |> Enum.reject(&(&1 == "latest.json"))
      |> Enum.map(fn filename ->
        full = Path.join(dir, filename)
        %{file: filename, stat: File.stat!(full)}
      end)
      |> Enum.sort_by(fn %{stat: stat} -> stat.mtime end, :desc)
      |> Enum.drop(keep)
      |> Enum.each(fn %{file: filename} -> File.rm(Path.join(dir, filename)) end)
    end
  end

  defp to_full_path(relative_path), do: Path.join(@base_dir, relative_path)
end
