defmodule Agglo.Content do
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
    Store.list_feeds()
  end

  def list_latest_posts do
    Store.list_posts()
  end

  def create_feed(attrs) do
    feed = struct(Feed, attrs)
    Store.add_feed(feed)
    {:ok, feed}
  end

  def create_post(attrs) do
    # Converte string de data para Date se necessário, ou mantém como está
    post = struct(Post, attrs)
    Store.add_post(post)
    {:ok, post}
  end
end