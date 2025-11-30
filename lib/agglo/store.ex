defmodule Agglo.Store do
  use Agent

  def start_link(_opts) do
    # Estado inicial: listas vazias
    Agent.start_link(fn -> %{feeds: [], posts: []} end, name: __MODULE__)
  end

  # --- FEEDS ---

  def list_feeds do
    Agent.get(__MODULE__, & &1.feeds)
  end

  def add_feed(feed) do
    Agent.update(__MODULE__, fn state ->
      # Adiciona se não existir (verificação simples por URL)
      if Enum.any?(state.feeds, &(&1.url == feed.url)) do
        state
      else
        Map.put(state, :feeds, [feed | state.feeds])
      end
    end)
  end

  # --- POSTS ---

  def list_posts do
    Agent.get(__MODULE__, fn state ->
      # Retorna ordenado por data (mais recente primeiro)
      state.posts
      |> Enum.sort_by(& &1.date, {:desc, Date})
    end)
  end

  def add_post(post) do
    Agent.update(__MODULE__, fn state ->
      # Evita duplicatas pela URL do post
      if Enum.any?(state.posts, &(&1.url == post.url)) do
	state
      else
	Map.put(state, :posts, [post | state.posts])
      end
    end)
  end
end
