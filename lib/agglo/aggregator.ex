defmodule Agglo.Aggregator do
  require Logger
  alias Agglo.Content
  # <--- Adicione o alias
  alias Agglo.Content.Parser

  def sync_all_feeds do
    feeds =
      Content.list_feeds()
      |> Task.async_stream(&sync_feed/1, timeout: 15_000)
      |> Stream.run()
  end

  def sync_feed(feed) do
    Logger.info("Sincronizando feed: #{feed.url}")

    # Req baixa o conteúdo
    case Req.get(feed.url) do
      {:ok, %{status: 200, body: body}} ->
	# Nosso parser processa o corpo
	entries = Parser.parse(body)

	Enum.each(entries, fn entry ->
	  create_post_from_entry(feed, entry)
	end)

      {:ok, %{status: status}} ->
	Logger.warning("Falha ao baixar #{feed.url}: Status #{status}")

      {:error, reason} ->
	Logger.error("Erro de rede em #{feed.url}: #{inspect(reason)}")
    end
  end

  defp create_post_from_entry(feed, entry) do
    attrs = %{
      title: entry.title,
      url: entry.url,
      content: sanitize(entry.content),
      published_at: entry.published_at,
      feed_id: feed.id
    }

    Content.create_post(attrs)
  end

  defp sanitize(html) do
    HtmlSanitizeEx.basic_html(html || "")
  end
end
