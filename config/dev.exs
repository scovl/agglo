import Config

# Configuração do Banco de Dados
config :agglo, Agglo.Repo,
  username: "postgres",
  password: "postgres",
  hostname: "localhost",
  database: "agglo_dev",
  stacktrace: true,
  show_sensitive_data_on_connection_error: true,
  pool_size: 10

# Configuração da Porta Web (4000)
config :agglo, AggloWeb.Endpoint,
  http: [ip: {127, 0, 0, 1}, port: 4000],
  check_origin: false,
  code_reloader: true,
  debug_errors: true,
  secret_key_base: "4qJ9L5pYt2X9r7M3v1K8n6B4c0Z2x5V8m1N7b3Q9w4E6r8T2y5U1i0O7p4A3s6D9",
  watchers: [] # Desabilitamos watchers de JS/CSS por enquanto para simplificar

# Live Reload
config :agglo, AggloWeb.Endpoint,
  live_reload: [
    patterns: [
      ~r"priv/static/.*(js|css|png|jpeg|jpg|gif|svg)$",
      ~r"lib/agglo_web/(controllers|live|components)/.*(ex|heex)$"
    ]
  ]

config :phoenix, :stacktrace_depth, 20
config :phoenix, :plug_init_mode, :runtime