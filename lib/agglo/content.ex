defmodule Agglo.Content do
  alias Agglo.FeedStorage
  alias Agglo.Feeds
  alias Agglo.Store

  # Estruturas simples (sem Ecto.Schema) para facilitar o uso no template
  defmodule Feed do
    defstruct [:url, :title]
  end

  defmodule Post do
    defstruct [:title, :url, :date, :feed_title]
  end

  # --- API PÚBLICA ---

  def list_feeds do
    Feeds.all()
  end

  def list_latest_posts do
    FeedStorage.list_latest_posts()
  end

  def create_feed(attrs) do
    feed = struct(Feed, attrs)
    Store.add_feed(feed)
    {:ok, feed}
  end

end