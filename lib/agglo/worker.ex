defmodule Agglo.Worker do
  use GenServer
  require Logger

  # Roda a cada 30 minutos
  @interval 30 * 60 * 1000 

  def start_link(_) do
    GenServer.start_link(__MODULE__, %{}, name: __MODULE__)
  end

  @impl true
  def init(state) do
    # Sincroniza logo ao iniciar (assíncrono para não travar o boot)
    send(self(), :sync)
    {:ok, state}
  end

  @impl true
  def handle_info(:sync, state) do
    Logger.info("Iniciando ciclo de atualização do Agglo...")
    Agglo.Aggregator.sync_all_feeds()
    
    # Agenda o próximo
    Process.send_after(self(), :sync, @interval)
    {:noreply, state}
  end
end