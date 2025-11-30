defmodule AggloWeb.PageController do
  use AggloWeb, :controller

  # Ação para a página inicial
  def home(conn, _params) do
    posts = Agglo.Content.list_latest_posts()
    render(conn, :home, posts: posts)
  end

  # --- A FUNÇÃO QUE FALTAVA ---
  # Ela precisa ser pública (def, não defp)
  def ignore_favicon(conn, _params) do
    send_resp(conn, 204, "")
  end
end