defmodule Agglo.Feeds do
  @moduledoc """
  Catálogo de feeds conhecidos pela aplicação.

  Combina uma lista padrão (configurável) com feeds adicionados
  em tempo de execução via `Agglo.Store`.
  """

  alias Agglo.Content.Feed
  alias Agglo.Store

  @default_feeds [
    %Feed{url: "https://gomex.me/blog/index.xml", title: "Gomex"},
    %Feed{url: "https://rss.slashdot.org/Slashdot/slashdot", title: "Slashdot"},
    %Feed{url: "https://www.theregister.com/headlines.atom", title: "The Register"},
    %Feed{url: "https://lobste.rs/rss", title: "Lobster.rs"}
  ]

  def all do
    dynamic_feeds = Store.list_feeds()

    (@default_feeds ++ dynamic_feeds)
    |> Enum.uniq_by(& &1.url)
  end
end
