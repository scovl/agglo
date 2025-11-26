defmodule Agglo.Content.Post do
  use Ecto.Schema
  import Ecto.Changeset

  schema "posts" do
    field :title, :string
    field :url, :string
    field :content, :string
    field :published_at, :utc_datetime
    belongs_to :feed, Agglo.Content.Feed

    timestamps()
  end

  def changeset(post, attrs) do
    post
    |> cast(attrs, [:title, :url, :content, :published_at, :feed_id])
    |> validate_required([:title, :url, :published_at, :feed_id])
    |> unique_constraint(:url)
  end
end