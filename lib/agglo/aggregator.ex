defmodule Agglo.Aggregator do
  require Logger
  alias Agglo.Content.Parser
  alias Agglo.FeedStorage
  alias Agglo.Feeds

  def sync_all_feeds do
    Feeds.all()
    |> Task.async_stream(&sync_feed/1, timeout: 15_000)
    |> Stream.run()
  end

  def sync_feed(feed) do
    Logger.info("Sincronizando feed: #{feed.url}")

    # Req baixa o conteúdo
    case Req.get(feed.url) do
      {:ok, %{status: 200, body: body}} ->
	# Nosso parser processa o corpo
        entries =
          body
          |> Parser.parse()
          |> Enum.map(&normalize_entry(&1, feed))

        if Enum.empty?(entries) do
          Logger.warning("Nenhum item encontrado em #{feed.url}")
        else
          FeedStorage.persist_feed_page(feed, entries)
        end

      {:ok, %{status: status}} ->
	Logger.warning("Falha ao baixar #{feed.url}: Status #{status}")

      {:error, reason} ->
	Logger.error("Erro de rede em #{feed.url}: #{inspect(reason)}")
    end
  end

  defp normalize_entry(entry, feed) do
    %{
      title: entry.title,
      url: entry.url,
      date: parse_date(entry.date),
      feed_title: feed.title
    }
  end

  defp parse_date(%Date{} = date), do: date
  defp parse_date(nil), do: Date.utc_today()

  defp parse_date(str) do
    case Date.from_iso8601(String.slice(str, 0, 10)) do
      {:ok, date} ->
        date

      {:error, _} ->
        parse_rss_date(str)
    end
  end

  defp parse_rss_date(str) do
    parts = String.split(str, " ")

    case parts do
      [_, day, month_str, year | _] ->
        convert_parts_to_date(day, month_str, year)

      [day, month_str, year | _] ->
        convert_parts_to_date(day, month_str, year)

      _ ->
        Logger.warning("Formato de data desconhecido: #{str}")
        Date.utc_today()
    end
  end

  defp convert_parts_to_date(day, month_str, year) do
    month =
      case month_str do
        "Jan" -> 1
        "Feb" -> 2
        "Mar" -> 3
        "Apr" -> 4
        "May" -> 5
        "Jun" -> 6
        "Jul" -> 7
        "Aug" -> 8
        "Sep" -> 9
        "Oct" -> 10
        "Nov" -> 11
        "Dec" -> 12
        _ -> 1
      end

    Date.new!(String.to_integer(year), month, String.to_integer(day))
  rescue
    _ -> Date.utc_today()
  end
end
