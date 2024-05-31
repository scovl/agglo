Aqui está o README atualizado para usar Boot ao invés de `deps.edn` e `clj`:

---

# Agglo

Agglo is a blog aggregator (a planet) that aggregates and displays updates from various blogs as feeds on specific topics.

### Prerequisites

- [Clojure](https://clojure.org/guides/getting_started)
- [Boot](https://boot-clj.github.io/)

### Installing

1. **Clone the repository:**

   ```bash
   git clone https://github.com/scovl/agglo.git
   ```

2. **Navigate to the project directory:**

   ```bash
   cd agglo
   ```

3. **Install dependencies:**

   Boot will automatically download and install the necessary dependencies when you run the tasks.

### Running the Application

To start the application, execute:

```bash
boot run
```

This will start a web server at `http://localhost:3000`.

### API Endpoints

- **`GET /`**: Welcome message.
- **`GET /feeds`**: Fetch and display the aggregated feeds.

### Built With

- [Clojure](https://clojure.org/) - The programming language used
- [Pedestal](https://github.com/pedestal/pedestal) - A set of libraries for building web applications in Clojure
- [clj-http](https://github.com/dakrone/clj-http) - HTTP client library for Clojure
- [Cheshire](https://github.com/dakrone/cheshire) - JSON encoding/decoding library for Clojure
