import { useEffect, useRef, useState } from "react";
import footerHeart from "./assets/footer-heart.png";
import pastelCatBackground from "./assets/pastel-cat-background.png";
import peekingCat from "./assets/peeking-cat.png";
import searchButtonPaw from "./assets/search-button-paw.png";
import suggestionCatLogo from "./assets/suggestion-cat-logo.png";
import titlePaw from "./assets/title-paw.png";

const DEBOUNCE_MS = 300;
const POST_SEARCH_REFRESH_MS = 6000;
const DEFAULT_PREFIX = "iph";
const TRENDING_PATH = "/trending?window=24h&limit=5";

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

function formatNumber(value) {
  return typeof value === "number" ? value.toLocaleString() : "\u2014";
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
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);

  const [trendingItems, setTrendingItems] = useState([]);
  const [isTrendingLoading, setIsTrendingLoading] = useState(true);
  const [trendingError, setTrendingError] = useState("");

  const latestRequestId = useRef(0);
  const suppressNextFetch = useRef(false);
  const postSearchRefreshTimeoutRef = useRef(null);

  const trimmedQuery = query.trim();
  const currentPrefix = trimmedQuery || DEFAULT_PREFIX;
  const showSuggestionsList = isDropdownOpen && !isLoading && suggestions.length > 0;
  const showPeekingCat = !showSuggestionsList;

  const fetchTrending = async () => {
    setIsTrendingLoading(true);
    setTrendingError("");

    try {
      const response = await fetch(TRENDING_PATH);
      const data = await parseJsonResponse(response);

      if (!response.ok) {
        throw new Error("Unable to fetch trending searches.");
      }

      setTrendingItems(Array.isArray(data.items) ? data.items.slice(0, 5) : []);
    } catch (error) {
      setTrendingItems([]);
      setTrendingError(error.message || "Unable to fetch trending searches.");
    } finally {
      setIsTrendingLoading(false);
    }
  };

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
      setIsDropdownOpen(false);
      setActiveSuggestionIndex(-1);
      return;
    }

    setIsLoading(true);
    setErrorMessage("");
    setIsDropdownOpen(true);
    setActiveSuggestionIndex(-1);

    try {
      const response = await fetch(`/suggest?q=${encodeURIComponent(normalizedPrefix)}`);
      const data = await parseJsonResponse(response);

      if (!response.ok) {
        throw new Error("Unable to fetch suggestions. Please try again.");
      }

      if (requestId !== latestRequestId.current) {
        return;
      }

      const nextSuggestions = Array.isArray(data.suggestions)
        ? data.suggestions.slice(0, 10)
        : [];

      setSuggestions(nextSuggestions);
      setSuggestionCount(nextSuggestions.length);
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
      setErrorMessage(error.message || "Unable to fetch suggestions. Please try again.");
    } finally {
      if (requestId === latestRequestId.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    document.title = "Search Typeahead System";
    fetchTrending();
  }, []);

  useEffect(() => {
    return () => {
      if (postSearchRefreshTimeoutRef.current) {
        window.clearTimeout(postSearchRefreshTimeoutRef.current);
      }
    };
  }, []);

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
      setIsDropdownOpen(false);
      setActiveSuggestionIndex(-1);
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

  const scheduleRefresh = (prefix) => {
    if (postSearchRefreshTimeoutRef.current) {
      window.clearTimeout(postSearchRefreshTimeoutRef.current);
    }

    postSearchRefreshTimeoutRef.current = window.setTimeout(() => {
      fetchSuggestions(prefix);
      fetchTrending();
    }, POST_SEARCH_REFRESH_MS);
  };

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
        throw new Error(data.message || "Unable to submit search.");
      }

      setQuery(normalizedQuery);
      setIsDropdownOpen(false);
      setActiveSuggestionIndex(-1);
      suppressNextFetch.current = true;
      setSuccessMessage(`Response: ${data.message || "Searched"}`);
      await Promise.all([fetchSuggestions(normalizedQuery), fetchTrending()]);
      scheduleRefresh(normalizedQuery);
    } catch (error) {
      setSuccessMessage("");
      setErrorMessage(error.message || "Backend is unavailable.");
    }
  };

  const handleSuggestionClick = async (suggestionQuery) => {
    setQuery(suggestionQuery);
    await handleSubmit(suggestionQuery);
  };

  const handleTrendingClick = async (trendQuery) => {
    setQuery(trendQuery);
    setSuccessMessage("");
    setErrorMessage("");
    setIsDropdownOpen(true);
    setActiveSuggestionIndex(-1);
    suppressNextFetch.current = true;
    await fetchSuggestions(trendQuery);
  };

  const handleFormSubmit = async (event) => {
    event.preventDefault();
    const selectedSuggestion = suggestions[activeSuggestionIndex];
    await handleSubmit(selectedSuggestion?.query || query);
  };

  const handleInputKeyDown = async (event) => {
    if (event.key === "ArrowDown") {
      if (!suggestions.length) {
        return;
      }

      event.preventDefault();
      setIsDropdownOpen(true);
      setActiveSuggestionIndex((currentIndex) =>
        currentIndex < suggestions.length - 1 ? currentIndex + 1 : 0
      );
      return;
    }

    if (event.key === "ArrowUp") {
      if (!suggestions.length) {
        return;
      }

      event.preventDefault();
      setIsDropdownOpen(true);
      setActiveSuggestionIndex((currentIndex) =>
        currentIndex > 0 ? currentIndex - 1 : suggestions.length - 1
      );
      return;
    }

    if (event.key === "Escape") {
      setActiveSuggestionIndex(-1);
      setIsDropdownOpen(false);
      return;
    }

    if (event.key === "Enter" && activeSuggestionIndex >= 0) {
      event.preventDefault();
      await handleSubmit(suggestions[activeSuggestionIndex].query);
    }
  };

  const showNoResults =
    Boolean(trimmedQuery) &&
    hasFetched &&
    !isLoading &&
    !errorMessage &&
    suggestions.length === 0;

  return (
    <main
      className="cute-page-shell"
      style={{ "--page-background": `url(${pastelCatBackground})` }}
    >
      <div className="page-overlay" />
      <section className="cute-search-page">
        <header className="cute-page-header">
          <h1>
            <img className="title-paw title-paw-left" src={titlePaw} alt="" />
            <span>Search Typeahead System</span>
            <img className="title-paw title-paw-right" src={titlePaw} alt="" />
          </h1>
          <p className="subtitle">
            Type a prefix to get suggestions, submit a search, and view trending
            searches.
          </p>
        </header>

        <form className="cute-search-form" onSubmit={handleFormSubmit}>
          <div className="search-bar-shell">
            <span className="magnifier-icon" aria-hidden="true" />
            <input
              id="search-query"
              className="search-input"
              type="text"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setSuccessMessage("");
                setIsDropdownOpen(true);
                setActiveSuggestionIndex(-1);
              }}
              onKeyDown={handleInputKeyDown}
              onFocus={() => {
                if (trimmedQuery) {
                  setIsDropdownOpen(true);
                }
              }}
              placeholder="Search queries like iph or spring boot"
              autoComplete="off"
              aria-autocomplete="list"
              aria-expanded={showSuggestionsList}
            />
            <button className="search-button" type="submit" disabled={!trimmedQuery}>
              <span>Search</span>
              <img className="search-button-paw" src={searchButtonPaw} alt="" />
            </button>
          </div>

          {successMessage ? <p className="feedback success">{successMessage}</p> : null}
          {errorMessage ? <p className="feedback error">{errorMessage}</p> : null}

          <p className="batch-note">
            New searches are batch-written, so fresh queries may appear after a few
            seconds.
          </p>

          <section className="suggestions-card">
            <div className="suggestions-header">
              <img className="suggestion-logo" src={suggestionCatLogo} alt="" />
              <div className="suggestions-heading">
                <h2>Suggestions for "{currentPrefix}"</h2>
                <p>{suggestionCount} shown</p>
              </div>
              {source !== "unknown" ? (
                <span className="source-note">Source: {source}</span>
              ) : null}
            </div>

            {isLoading ? <p className="state-message">Loading suggestions...</p> : null}

            {!isLoading && !trimmedQuery ? (
              <p className="state-message">
                Start typing to fetch suggestions from the backend.
              </p>
            ) : null}

            {showNoResults ? (
              <p className="state-message">No suggestions found for this query.</p>
            ) : null}

            {!isLoading && errorMessage ? (
              <p className="state-message">Unable to fetch suggestions. Please try again.</p>
            ) : null}

            {showSuggestionsList ? (
              <ul className="suggestions-list" role="listbox">
                {suggestions.map((suggestion, index) => (
                  <li key={suggestion.query}>
                    <button
                      type="button"
                      className={`suggestion-item${
                        activeSuggestionIndex === index ? " active" : ""
                      }`}
                      onClick={() => handleSuggestionClick(suggestion.query)}
                    >
                      <span className="suggestion-query">{suggestion.query}</span>
                      <span className="count-badge">
                        {formatNumber(suggestion.count)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}

            {showPeekingCat ? <img className="peeking-cat" src={peekingCat} alt="" /> : null}
          </section>
        </form>

        <section className="trending-card">
          <div className="trending-header">
            <span className="trend-header-icon" aria-hidden="true">
              ↗
            </span>
            <div>
              <h2>Trending searches</h2>
              <p>Top 5 from recent activity</p>
            </div>
          </div>

          {isTrendingLoading ? (
            <p className="state-message">Loading trending searches...</p>
          ) : null}

          {!isTrendingLoading && trendingError ? (
            <p className="state-message">Unable to fetch trending searches.</p>
          ) : null}

          {!isTrendingLoading && !trendingError && trendingItems.length === 0 ? (
            <p className="state-message">No trending searches yet.</p>
          ) : null}

          {!isTrendingLoading && !trendingError && trendingItems.length > 0 ? (
            <ul className="trending-list">
              {trendingItems.map((item) => (
                <li key={item.query}>
                  <button
                    type="button"
                    className="trending-item"
                    onClick={() => handleTrendingClick(item.query)}
                  >
                    <span className="trend-pill" aria-hidden="true">
                      ↗
                    </span>
                    <div className="trending-item-copy">
                      <span className="trending-query">{item.query}</span>
                      <p>
                        Recent {formatNumber(item.recentCount)} · Total{" "}
                        {formatNumber(item.totalCount)}
                      </p>
                    </div>
                    <span className="trend-chevron" aria-hidden="true">
                      &gt;
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </section>

        <footer className="page-footer">
          <img className="footer-heart" src={footerHeart} alt="" />
          <span>Created by Antara Utane</span>
          <img className="footer-heart" src={footerHeart} alt="" />
        </footer>
      </section>
    </main>
  );
}

export default App;
