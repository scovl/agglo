# Agglo

Agglo is a blog aggregator (a planet) that aggregates and displays updates from various blogs as feeds on specific topics.

### Prerequisites

- [Clojure](https://clojure.org/guides/getting_started)
- [Leiningen](https://leiningen.org/)

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

   Leiningen will automatically download and install the necessary dependencies when you run the tasks.

### Configuration

The application uses two main configuration files:

1. **`resources/config.edn`**: Contains RSS feed URLs and other application settings
2. **`resources/logback.xml`**: Configures logging behavior for both console and file output

Logs are written to `logs/app.log`.

### Running the Application

Compile the ClojureScript assets and start the application:

```bash
lein cljsbuild once
lein run
```

This will start a web server at `http://localhost:8080`.

### API Endpoints

- **`GET /`**: Displays the aggregated RSS feeds in a responsive web interface.

### Built With

- [Clojure](https://clojure.org/) - The programming language used
- [Pedestal](https://github.com/pedestal/pedestal) - A set of libraries for building web applications in Clojure
- [clj-http](https://github.com/dakrone/clj-http) - HTTP client library for Clojure
- [Selmer](https://github.com/yogthos/Selmer) - A fast, Django-inspired template system for Clojure
- [Logback](https://logback.qos.ch/) - Logging framework for Java/Clojure
