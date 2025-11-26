defp deps do
  [
    {:phoenix, "~> 1.7"},
    {:phoenix_ecto, "~> 4.4"},
    {:ecto_sql, "~> 3.10"},
    {:postgrex, ">= 0.0.0"},
    {:phoenix_html, "~> 4.0"},
    {:phoenix_live_reload, "~> 1.2", only: :dev},
    {:phoenix_live_view, "~> 0.20.0"},
    {:floki, ">= 0.30.0"},
    {:esbuild, "~> 0.8", runtime: Mix.env() == :dev},
    {:tailwind, "~> 0.2", runtime: Mix.env() == :dev},
    {:jason, "~> 1.2"},
    {:dns_cluster, "~> 0.1.1"},
    {:bandit, "~> 1.0"},
    # Adicione estas linhas:
    {:req, "~> 0.4.0"},
    {:feeder_ex, "~> 1.1"},
    {:html_sanitize_ex, "~> 1.4"}
  ]
end