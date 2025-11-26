defmodule Agglo.Content do
  import Ecto.Query, warn: false
  alias Agglo.Repo
  alias Agglo.Content.{Feed, Post}

  # --- Feeds ---
  def list_feeds, do: Repo.all(Feed)

  def create_feed(attrs \\ %{}) do
    %Feed{}
    |> Feed.changeset(attrs)
    |> Repo.insert()
  end

  # --- Posts ---
  def list_latest_posts(limit \\ 50) do
    from(p in Post,
      order_by: [desc: p.published_at],
      preload: [:feed],
      limit: ^limit
    )
    |> Repo.all()
  end

  # Função para salvar posts ignorando duplicatas (upsert ou ignore)
  def create_post(attrs) do
    %Post{}
    |> Post.changeset(attrs)
    |> Repo.insert(on_conflict: :nothing) # Se a URL já existe, ignora
  end
end