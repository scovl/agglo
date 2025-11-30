defmodule Agglo.Content.Parser do
  import SweetXml

  def parse(xml_string) do
    # 1. Tenta formato ATOM (ex: Elixir Lang)
    entries = xml_string
    |> xpath(
      ~x"//entry"l,
      title: ~x"title/text()"s,
      url: ~x"link/@href"s,
      date: ~x"published/text()"s # Atom costuma usar published ou updated
    )

    if Enum.empty?(entries) do
      # 2. Se vazio, tenta formato RSS (ex: Slashdot, Gomex)
      xml_string
      |> xpath(
        ~x"//item"l,
        title: ~x"title/text()"s,
        url: ~x"link/text()"s, # RSS usa o texto dentro da tag, não atributo href
        date: ~x"pubDate/text()"s # RSS usa pubDate
      )
    else
      entries
    end
  end
end