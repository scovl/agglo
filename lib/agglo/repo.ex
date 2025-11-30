defmodule Agglo.Repo do
  use Ecto.Repo,
    otp_app: :agglo,
    adapter: Ecto.Adapters.Postgres
end