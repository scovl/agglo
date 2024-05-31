# Agglo

Agglo is a blog aggregator (a planet) that aggregates and displays updates from various blogs as feeds on specific topics.

### Prerequisites

- [Clojure](https://clojure.org/guides/getting_started)
- [Leiningen](https://leiningen.org/) (optional, if you prefer using Leiningen)

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

   If you are using `deps.edn`, simply run:

   ```bash
   clj -A:deps
   ```

### Running the Application

To start the application, execute:

```bash
clj -M -m agglo.core
```

This will start a web server at `http://localhost:3000`.

### API Endpoints

- **`GET /`**: Welcome message.
- **`GET /feeds`**: Fetch and display the aggregated feeds.


### Built With

- [Clojure](https://clojure.org/) - The programming language used
- [Ring](https://github.com/ring-clojure/ring) - Clojure HTTP server library
- [clj-http](https://github.com/dakrone/clj-http) - HTTP client library for Clojure
- [Cheshire](https://github.com/dakrone/cheshire) - JSON encoding/decoding library for Clojure