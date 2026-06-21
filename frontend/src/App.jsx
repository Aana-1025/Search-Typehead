import { useEffect, useRef, useState } from "react";

const DEBOUNCE_MS = 300;

async function parseJsonResponse(response) {
  const text = await response.text();

  if (!text) {
    return {};
  }

  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
}

function App() {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [suggestionCount, setSuggestionCount] = useState(0);
  const [source, setSource] = useState("unknown");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [hasFetched, setHasFetched] = useState(false);
  const latestRequestId = useRef(0);
  const suppressNextFetch = useRef(false);

  const trimmedQuery = query.trim();

  const fetchSuggestions = async (prefix) => {
    const normalizedPrefix = prefix.trim();
    const requestId = latestRequestId.current + 1;
    latestRequestId.current = requestId;

    if (!normalizedPrefix) {
      setSuggestions([]);
      setSuggestionCount(0);
      setSource("unknown");
      setIsLoading(false);
      setErrorMessage("");
      setHasFetched(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage("");

    try {
      const response = await fetch(`/suggest?q=${encodeURIComponent(normalizedPrefix)}`);
      const data = await parseJsonResponse(response);

      if (!response.ok) {
        throw new Error("Unable to fetch suggestions right now.");
      }

      if (requestId !== latestRequestId.current) {
        return;
      }

      setSuggestions(Array.isArray(data.suggestions) ? data.suggestions : []);
      setSuggestionCount(
        typeof data.count === "number"
          ? data.count
          : Array.isArray(data.suggestions)
            ? data.suggestions.length
            : 0
      );
      setSource(data.source || "unknown");
      setHasFetched(true);
    } catch (error) {
      if (requestId !== latestRequestId.current) {
        return;
      }

      setSuggestions([]);
      setSuggestionCount(0);
      setSource("unavailable");
      setHasFetched(true);
      setErrorMessage(error.message || "Backend is unavailable.");
    } finally {
      if (requestId === latestRequestId.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    if (!trimmedQuery) {
      latestRequestId.current += 1;
      suppressNextFetch.current = false;
      setSuggestions([]);
      setSuggestionCount(0);
      setSource("unknown");
      setIsLoading(false);
      setErrorMessage("");
      setHasFetched(false);
      return undefined;
    }

    if (suppressNextFetch.current) {
      suppressNextFetch.current = false;
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      fetchSuggestions(trimmedQuery);
    }, DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [trimmedQuery]);

  const handleSubmit = async (value) => {
    const normalizedQuery = value.trim();

    if (!normalizedQuery) {
      return;
    }

    setSuccessMessage("");
    setErrorMessage("");

    try {
      const response = await fetch("/search", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ query: normalizedQuery })
      });
      const data = await parseJsonResponse(response);

      if (!response.ok) {
        throw new Error(data.message || "Unable to submit search right now.");
      }

      setQuery(normalizedQuery);
      suppressNextFetch.current = true;
      setSuccessMessage(data.message || "Searched");
      await fetchSuggestions(normalizedQuery);
    } catch (error) {
      setSuccessMessage("");
      setErrorMessage(error.message || "Backend is unavailable.");
    }
  };

  const handleSuggestionClick = async (suggestionQuery) => {
    setQuery(suggestionQuery);
    await handleSubmit(suggestionQuery);
  };

  const handleFormSubmit = async (event) => {
    event.preventDefault();
    await handleSubmit(query);
  };

  const showNoResults =
    Boolean(trimmedQuery) &&
    hasFetched &&
    !isLoading &&
    !errorMessage &&
    suggestions.length === 0;

  return (
    <main className="app-shell">
      <section className="hero-card">
        <div className="hero-copy">
          <p className="eyebrow">Milestone 7</p>
          <h1>Search Typeahead System</h1>
          <p className="hero-text">
            Type a prefix, inspect live PostgreSQL-backed suggestions, and submit a
            search to see the refreshed counts flow back into the UI.
          </p>
        </div>

        <form className="search-panel" onSubmit={handleFormSubmit}>
          <label className="input-label" htmlFor="search-query">
            Search query
          </label>

          <div className="search-row">
            <input
              id="search-query"
              className="search-input"
              type="text"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setSuccessMessage("");
              }}
              placeholder="Try iph, spring boot, or a new query"
              autoComplete="off"
            />
            <button className="search-button" type="submit" disabled={!trimmedQuery}>
              Search
            </button>
          </div>

          <div className="status-row" aria-live="polite">
            <span>
              Source: <strong>{source}</strong>
            </span>
            <span>
              Suggestions: <strong>{suggestionCount}</strong>
            </span>
          </div>

          {successMessage ? <p className="feedback success">{successMessage}</p> : null}
          {errorMessage ? <p className="feedback error">{errorMessage}</p> : null}

          <div className="suggestions-card">
            {isLoading ? <p className="state-message">Loading suggestions...</p> : null}

            {!isLoading && !trimmedQuery ? (
              <p className="state-message">
                Start typing to fetch suggestions from the backend.
              </p>
            ) : null}

            {showNoResults ? (
              <p className="state-message">No suggestions found for this query.</p>
            ) : null}

            {!isLoading && suggestions.length > 0 ? (
              <ul className="suggestions-list">
                {suggestions.map((suggestion) => (
                  <li key={suggestion.query}>
                    <button
                      type="button"
                      className="suggestion-item"
                      onClick={() => handleSuggestionClick(suggestion.query)}
                    >
                      <span className="suggestion-query">{suggestion.query}</span>
                      <span className="suggestion-count">{suggestion.count}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </form>
      </section>
    </main>
  );
}

export default App;
