defmodule Agglo.Store do
  @moduledoc """
  Agente simples para armazenar metadados de feeds cadastrados
  pelo usuário. O conteúdo dos feeds (posts) é materializado
  em storage de objetos, não em memória.
  """

  use Agent

  def start_link(_opts) do
    # Estado inicial: apenas feeds registrados pelo usuário
    Agent.start_link(fn -> %{feeds: []} end, name: __MODULE__)
  end

  @doc """
  Lista os feeds adicionados em tempo de execução (além dos padrão).
  """
  def list_feeds do
    Agent.get(__MODULE__, & &1.feeds)
  end

  @doc """
  Registra um novo feed se ainda não existir (comparando por URL).
  """
  def add_feed(feed) do
    Agent.update(__MODULE__, fn state ->
      if Enum.any?(state.feeds, &(&1.url == feed.url)) do
        state
      else
        Map.put(state, :feeds, [feed | state.feeds])
      end
    end)
  end
end
