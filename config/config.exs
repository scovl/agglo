import Config

config :agglo,
  ecto_repos: [Agglo.Repo],
  generators: [timestamp_type: :utc_datetime]

# Configura o Endpoint (Servidor Web)
config :agglo, AggloWeb.Endpoint,
  url: [host: "localhost"],
  adapter: Bandit.PhoenixAdapter,
  render_errors: [
    formats: [json: AggloWeb.ErrorJSON], # Simplificado para evitar erros de HTML agora
    layout: false
  ],
  pubsub_server: Agglo.PubSub,
  live_view: [signing_salt: "J8kLS9d+"] # Salt aleatório

# Configurações de Assets (para calar os warnings)
config :esbuild,
  version: "0.25.0",
  agglo: [
    args: ~w(js/app.js --bundle --target=es2017 --outdir=../priv/static/assets --external:/fonts/* --external:/images/*),
    cd: Path.expand("../assets", __DIR__),
    env: %{"NODE_PATH" => Path.expand("../deps", __DIR__)}
  ]

config :tailwind,
  version: "4.1.12",
  agglo: [
    args: ~w(--config=tailwind.config.js --input=css/app.css --output=../priv/static/assets/app.css),
    cd: Path.expand("../assets", __DIR__)
  ]

config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

config :phoenix, :json_library, Jason

# Importa a configuração específica do ambiente (dev, test, prod)
import_config "#{config_env()}.exs"