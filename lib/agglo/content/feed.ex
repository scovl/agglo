defmodule Agglo.Content.Feed do
  use Ecto.Schema
  import Ecto.Changeset

  schema "feeds" do
    field :title, :string
    field :url, :string
    field :site_url, :string
    has_many :posts, Agglo.Content.Post

    timestamps()
  end

  def changeset(feed, attrs) do
    feed
    |> cast(attrs, [:title, :url, :site_url])
    |> validate_required([:url])
    |> unique_constraint(:url)
  end
end