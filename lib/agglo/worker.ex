defmodule Agglo.Worker do
  use GenServer
  require Logger

  # --- CONFIGURAÇÃO ---
  @feeds [
    {"https://gomex.me/blog/index.xml", "Gomex"},
    {"https://rss.slashdot.org/Slashdot/slashdot", "Slashdot"},
    {"https://www.theregister.com/headlines.atom", "The Register"},
    {"https://lobste.rs/rss", "Lobster.rs"}
  ]

  # --------------------

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
    Enum.each(@feeds, fn {url, feed_name} ->
      fetch_feed(url, feed_name)
    end)

    {:noreply, state}
  end

  defp fetch_feed(url, feed_name) do
    Logger.info("📡 Verificando #{feed_name}...")

    case Req.get(url) do
      {:ok, response} ->
	entries = Agglo.Content.Parser.parse(response.body)
	latest_entry = List.first(entries)

	if latest_entry do
	  # AQUI MUDOU: Usamos nossa nova função inteligente de data
	  date = parse_date_string(latest_entry.date)

	  Agglo.Store.add_post(%Agglo.Content.Post{
	    title: latest_entry.title,
	    url: latest_entry.url,
	    date: date,
	    feed_title: feed_name
	  })

	  Logger.info("✅ [#{feed_name}] Salvo: #{latest_entry.title} (#{date})")
	else
	  Logger.warning("⚠️ [#{feed_name}] Sem posts.")
	end

      {:error, _} ->
	Logger.error("❌ Erro ao baixar #{feed_name}")
    end
  end

  # --- PARSERS DE DATA MANUAIS (SEM BIBLIOTECA EXTRA) ---

  defp parse_date_string(nil), do: Date.utc_today()

  defp parse_date_string(str) do
    # Tenta primeiro o formato ISO (Atom/Elixir): "2025-11-26T..."
    case Date.from_iso8601(String.slice(str, 0, 10)) do
      {:ok, date} ->
	date

      {:error, _} ->
	# Se falhar, tenta formato RSS: "Wed, 26 Nov 2025..."
	parse_rss_date(str)
    end
  end

  defp parse_rss_date(str) do
    # Quebra a string por espaços.
    # Ex RSS: "Wed, 26 Nov 2025 12:00:00 GMT" -> ["Wed,", "26", "Nov", "2025", ...]
    parts = String.split(str, " ")

    case parts do
      # Formato padrão: DiaSemana, Dia, Mes, Ano, ...
      [_, day, month_str, year | _] ->
	convert_parts_to_date(day, month_str, year)

      # Caso venha sem o dia da semana: Dia, Mes, Ano...
      [day, month_str, year | _] ->
	convert_parts_to_date(day, month_str, year)

      _ ->
	Logger.warning("Formato de data desconhecido: #{str}")
	Date.utc_today()
    end
  end

  defp convert_parts_to_date(day, month_str, year) do
    month =
      case month_str do
	"Jan" -> 1
	"Feb" -> 2
	"Mar" -> 3
	"Apr" -> 4
	"May" -> 5
	"Jun" -> 6
	"Jul" -> 7
	"Aug" -> 8
	"Sep" -> 9
	"Oct" -> 10
	"Nov" -> 11
	"Dec" -> 12
	_ -> 1
      end

    # Converte strings para inteiros e cria a Data
    Date.new!(String.to_integer(year), month, String.to_integer(day))
  rescue
    _ -> Date.utc_today()
  end
end
