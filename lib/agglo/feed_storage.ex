defmodule Agglo.FeedStorage do
  @moduledoc """
  Materializa páginas de feed como objetos JSON efêmeros.

  Cada feed possui um manifest (`latest.json`) que aponta para
  a página mais recente, permitindo entrega via CDN/object storage
  sem depender de memória ou de um banco relacional.
  """

  alias Agglo.Content.Post
  alias Agglo.Feeds
  alias Agglo.ObjectStorage

  @retention Application.compile_env(:agglo, :object_storage_retention, 3)

  def persist_feed_page(feed, entries) do
    generated_at = DateTime.utc_now() |> DateTime.truncate(:second)
    slug = slugify(feed.title || feed.url)
    key = "feeds/#{slug}/page-#{DateTime.to_unix(generated_at)}.json"

    payload = %{
      feed: %{title: feed.title, url: feed.url},
      generated_at: DateTime.to_iso8601(generated_at),
      items: Enum.map(entries, &serialize_entry/1)
    }

    manifest_key = "feeds/#{slug}/latest.json"

    ObjectStorage.write_json!(key, payload)
    ObjectStorage.write_json!(manifest_key, %{latest: key, generated_at: payload.generated_at})
    ObjectStorage.cleanup_prefix!("feeds/#{slug}", keep: @retention)

    {:ok, key}
  end

  def latest_page(feed) do
    slug = slugify(feed.title || feed.url)
    manifest_key = "feeds/#{slug}/latest.json"

    with {:ok, %{"latest" => latest_key}} <- ObjectStorage.read_json(manifest_key),
         {:ok, page} <- ObjectStorage.read_json(latest_key) do
      {:ok, page}
    else
      _ -> {:error, :not_found}
    end
  end

  def list_latest_posts(feeds \ Feeds.all()) do
    feeds
    |> Enum.flat_map(&posts_from_feed/1)
    |> Enum.sort_by(& &1.date, {:desc, Date})
  end

  defp posts_from_feed(feed) do
    case latest_page(feed) do
      {:ok, %{"items" => items}} ->
        Enum.map(items, fn item ->
          date =
            case Date.from_iso8601(item["date"]) do
              {:ok, parsed} -> parsed
              _ -> Date.utc_today()
            end

          struct(Post, %{
            title: item["title"],
            url: item["url"],
            date: date,
            feed_title: item["feed_title"]
          })
        end)

      _ -> []
    end
  end

  defp slugify(name) do
    name
    |> String.downcase()
    |> String.replace(~r/[^a-z0-9]+/u, "-")
    |> String.trim("-")
  end

  defp serialize_entry(%{date: %Date{} = date} = entry) do
    entry
    |> Map.put(:date, Date.to_iso8601(date))
    |> Map.take([:title, :url, :date, :feed_title])
  end
end
