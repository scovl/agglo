defmodule AggloWeb.PageController do
  use AggloWeb, :controller
  alias Agglo.Content

  def home(conn, _params) do
    posts = Content.list_latest_posts()
    render(conn, :home, posts: posts)
  end
end