def start(_type, _args) do
  children = [
    AggloWeb.Telemetry,
    Agglo.Repo,
    {DNSCluster, query: Application.get_env(:agglo, :dns_cluster_query) || :ignore},
    {Phoenix.PubSub, name: Agglo.PubSub},
    # Adicione seu worker aqui:
    Agglo.Worker,
    AggloWeb.Endpoint
  ]
