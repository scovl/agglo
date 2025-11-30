defmodule Agglo.Worker do
  use GenServer
  require Logger

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, nil, name: __MODULE__)
  end

  @impl true
  def init(_) do
    Logger.info("🤖 Worker iniciado! Processando lista de feeds...")
    send(self(), :fetch_rss)
    {:ok, nil}
  end

  @impl true
  def handle_info(:fetch_rss, state) do
    Agglo.Aggregator.sync_all_feeds()

    {:noreply, state}
  end
end
