defmodule Agglo.Application do
  use Application

  @impl true
  def start(_type, _args) do
    children = [
      AggloWeb.Telemetry,
      {DNSCluster, query: Application.get_env(:agglo, :dns_cluster_query) || :ignore},
      {Phoenix.PubSub, name: Agglo.PubSub},

      # 1º: A Memória inicia
      Agglo.Store,

      # 2º: O Worker inicia (e usa a memória)
      Agglo.Worker,
      AggloWeb.Endpoint
    ]

    opts = [strategy: :one_for_one, name: Agglo.Supervisor]
    Supervisor.start_link(children, opts)
  end

  @impl true
  def config_change(changed, _new, removed) do
    AggloWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
