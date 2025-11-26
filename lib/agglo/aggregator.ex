defmodule Agglo.Aggregator do
  require Logger
  alias Agglo.Content

  def sync_all_feeds do
    feeds = Content.list_feeds()
    
    # Processa em paralelo para ser rápido
    feeds
    |> Task.async_stream(&sync_feed/1, timeout: 15_000) 
    |> Stream.run()
  end

  def sync_feed(feed) do
    Logger.info("Sincronizando feed: #{feed.url}")

    with {:ok, response} <- Req.get(feed.url),
         {:ok, parsed_feed, _entries} <- FeederEx.parse(response.body) do
      
      # Atualiza metadados do feed se necessário (opcional)
      # ...

      parsed_feed.entries
      |> Enum.each(fn entry -> create_post_from_entry(feed, entry) end)
    else
      error -> Logger.error("Erro ao sincronizar #{feed.url}: #{inspect(error)}")
    end
  end

  defp create_post_from_entry(feed, entry) do
    # RSS/Atom tem formatos de data variados. FeederEx tenta normalizar,
    # mas às vezes precisamos de parsing extra. Vamos assumir o padrão aqui.
    {:ok, published_at, _} = DateTime.from_iso8601(entry.updated || entry.published || DateTime.utc_now() |> DateTime.to_iso8601())

    attrs = %{
      title: entry.title,
      url: entry.id || entry.link, # Atom usa ID, RSS usa Link
      content: sanitize(entry.summary || entry.description || ""),
      published_at: published_at,
      feed_id: feed.id
    }

    Content.create_post(attrs)
  end

  defp sanitize(html) do
    # Remove scripts e iframes maliciosos
    HtmlSanitizeEx.basic_html(html)
  end
end