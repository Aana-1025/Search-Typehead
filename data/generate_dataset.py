import argparse
import csv
import random
from collections import OrderedDict
from pathlib import Path


def normalize_query(query: str) -> str:
    return " ".join(query.strip().lower().split())


def add_query(pool: OrderedDict[str, str], query: str) -> None:
    normalized = normalize_query(query)
    if normalized and normalized not in pool:
        pool[normalized] = " ".join(query.strip().split())


def build_query_pool() -> OrderedDict[str, str]:
    pool: OrderedDict[str, str] = OrderedDict()

    products = ["iphone", "laptop", "shoes", "headphones", "charger", "watch", "bag"]
    programming = ["java", "python", "spring boot", "react", "docker", "postgresql"]
    food = ["pizza", "burger", "sushi", "biryani", "coffee", "cake"]
    cities = ["bangalore", "delhi", "mumbai", "pune", "hyderabad"]
    entertainment = ["netflix", "cricket", "movies", "anime", "music"]
    learning_topics = ["tutorial", "course", "roadmap", "interview questions"]

    popular_queries = [
        "iphone",
        "iphone 15",
        "iphone 15 pro max",
        "java",
        "java tutorial",
        "python",
        "python tutorial",
        "laptop",
        "best laptop for students",
        "shoes",
        "running shoes for men",
        "pizza",
        "pizza near me",
        "netflix",
        "cricket score",
        "bangalore weather",
        "spring boot",
        "spring boot tutorial",
        "react tutorial",
        "docker tutorial",
        "postgresql tutorial",
        "biryani near me",
        "coffee near me",
        "anime movies",
        "music playlist",
    ]
    for query in popular_queries:
        add_query(pool, query)

    budgets = [
        "299", "499", "799", "999", "1499", "1999", "2499", "2999", "3999", "4999",
        "6999", "9999", "14999", "19999", "24999", "29999", "39999", "49999", "69999",
        "99999",
    ]
    years = ["2023", "2024", "2025", "2026"]
    audiences = [
        "students", "beginners", "professionals", "women", "men", "kids",
        "gamers", "travelers", "office use", "daily use",
    ]
    comparison_modifiers = ["best", "top rated", "budget", "premium", "latest"]
    shopping_intents = [
        "price", "review", "offers", "discount", "online", "delivery", "exchange offer",
        "emi options", "store near me", "sale",
    ]
    product_features = [
        "for gaming", "for college", "for office", "for travel", "with fast charging",
        "with long battery life", "with warranty", "for everyday use", "for gifting",
        "for outdoor use",
    ]
    product_brands = {
        "iphone": ["iphone 13", "iphone 14", "iphone 15", "iphone 15 plus", "iphone 15 pro", "iphone 15 pro max"],
        "laptop": ["hp laptop", "dell laptop", "lenovo laptop", "asus laptop", "macbook air", "macbook pro"],
        "shoes": ["nike shoes", "adidas shoes", "puma shoes", "reebok shoes", "sports shoes", "casual shoes"],
        "headphones": ["sony headphones", "jbl headphones", "boat headphones", "wireless headphones", "bluetooth headphones"],
        "charger": ["fast charger", "wireless charger", "usb c charger", "iphone charger", "laptop charger"],
        "watch": ["smart watch", "fitness watch", "digital watch", "analog watch", "apple watch"],
        "bag": ["school bag", "travel bag", "laptop bag", "office bag", "gym bag"],
    }

    for term in products:
        add_query(pool, term)
        for modifier in comparison_modifiers:
            add_query(pool, f"{modifier} {term}")
            add_query(pool, f"{modifier} {term} in india")
        for intent in shopping_intents:
            add_query(pool, f"{term} {intent}")
            for city in cities:
                add_query(pool, f"{term} {intent} in {city}")
        for year in years:
            add_query(pool, f"{term} {year}")
            add_query(pool, f"best {term} {year}")
        for audience in audiences:
            add_query(pool, f"{term} for {audience}")
            add_query(pool, f"best {term} for {audience}")
            for city in cities:
                add_query(pool, f"{term} for {audience} in {city}")
        for feature in product_features:
            add_query(pool, f"{term} {feature}")
            add_query(pool, f"best {term} {feature}")
        for budget in budgets:
            add_query(pool, f"{term} under {budget}")
            add_query(pool, f"best {term} under {budget}")
            for city in cities:
                add_query(pool, f"{term} under {budget} in {city}")
                for audience in audiences:
                    add_query(pool, f"{term} under {budget} for {audience} in {city}")
                    add_query(pool, f"best {term} under {budget} for {audience} in {city}")
        for branded in product_brands.get(term, []):
            add_query(pool, branded)
            for intent in shopping_intents:
                add_query(pool, f"{branded} {intent}")
            for budget in budgets:
                add_query(pool, f"{branded} under {budget}")
                for city in cities:
                    add_query(pool, f"{branded} under {budget} in {city}")

    skill_levels = ["for beginners", "for experienced developers", "for freshers", "step by step", "advanced guide"]
    roles = [
        "backend developer", "frontend developer", "full stack developer", "software engineer",
        "data engineer", "devops engineer", "student", "interview prep",
    ]
    programming_goals = [
        "tutorial", "course", "roadmap", "interview questions", "projects", "project ideas",
        "examples", "cheatsheet", "best practices", "installation guide",
    ]
    programming_tools = [
        "vscode", "intellij", "maven", "gradle", "docker", "postgresql", "rest api",
        "microservices", "hibernate", "jpa",
    ]

    for term in programming:
        add_query(pool, term)
        for goal in programming_goals:
            add_query(pool, f"{term} {goal}")
            for year in years:
                add_query(pool, f"{term} {goal} {year}")
        for level in skill_levels:
            add_query(pool, f"{term} tutorial {level}")
            add_query(pool, f"{term} course {level}")
        for role in roles:
            add_query(pool, f"{term} roadmap for {role}")
            add_query(pool, f"{term} interview questions for {role}")
            add_query(pool, f"{term} projects for {role}")
        for tool in programming_tools:
            add_query(pool, f"{term} with {tool}")
            add_query(pool, f"{term} {tool} tutorial")
        for other in programming:
            if other != term:
                add_query(pool, f"{term} vs {other}")
                add_query(pool, f"{term} with {other}")
        for city in cities:
            add_query(pool, f"{term} classes in {city}")
            add_query(pool, f"{term} training in {city}")

    food_intents = [
        "near me", "delivery", "takeaway", "offers", "menu", "recipe", "places near me",
        "best place for", "late night", "open now",
    ]
    meal_times = ["breakfast", "lunch", "dinner", "snacks", "weekend"]
    food_descriptors = [
        "best", "spicy", "family", "veg", "non veg", "authentic", "cheap", "premium",
        "unlimited", "homemade",
    ]

    for term in food:
        add_query(pool, term)
        for intent in food_intents:
            add_query(pool, f"{term} {intent}")
            for city in cities:
                add_query(pool, f"{term} {intent} in {city}")
        for descriptor in food_descriptors:
            add_query(pool, f"{descriptor} {term}")
            for city in cities:
                add_query(pool, f"{descriptor} {term} in {city}")
        for meal in meal_times:
            add_query(pool, f"{term} for {meal}")
            for city in cities:
                add_query(pool, f"{term} for {meal} in {city}")
        for budget in budgets[:12]:
            add_query(pool, f"{term} under {budget}")
            for city in cities:
                add_query(pool, f"{term} under {budget} in {city}")

    city_topics = [
        "weather", "jobs", "restaurants", "traffic update", "news", "airport", "metro map",
        "places to visit", "hotels", "rent", "shopping malls", "coworking space",
        "weekend getaway", "schools", "it companies", "best cafes",
    ]
    city_modifiers = ["best", "cheap", "luxury", "family friendly", "new"]

    for city in cities:
        add_query(pool, city)
        for topic in city_topics:
            add_query(pool, f"{city} {topic}")
            for year in years:
                add_query(pool, f"{city} {topic} {year}")
        for modifier in city_modifiers:
            for topic in city_topics:
                add_query(pool, f"{modifier} {topic} in {city}")
        for budget in budgets[:12]:
            add_query(pool, f"hotels in {city} under {budget}")
            add_query(pool, f"restaurants in {city} under {budget}")
            add_query(pool, f"places to stay in {city} under {budget}")

    entertainment_intents = [
        "shows", "movies", "songs", "playlist", "highlights", "schedule", "score",
        "streaming", "subscription", "updates", "news", "best of",
    ]
    entertainment_modifiers = [
        "latest", "trending", "best", "top", "new", "family", "weekend", "popular",
    ]

    for term in entertainment:
        add_query(pool, term)
        for intent in entertainment_intents:
            add_query(pool, f"{term} {intent}")
            for year in years:
                add_query(pool, f"{term} {intent} {year}")
        for modifier in entertainment_modifiers:
            add_query(pool, f"{modifier} {term}")
            add_query(pool, f"{modifier} {term} in india")
        for city in cities:
            add_query(pool, f"{term} events in {city}")
            add_query(pool, f"{term} updates in {city}")

    learning_subjects = products + programming + food + entertainment + cities
    learning_levels = ["for beginners", "for intermediate learners", "for advanced learners", "step by step", "quick guide"]

    for subject in learning_subjects:
        for topic in learning_topics:
            add_query(pool, f"{subject} {topic}")
            for level in learning_levels:
                add_query(pool, f"{subject} {topic} {level}")
            for year in years:
                add_query(pool, f"{subject} {topic} {year}")
            for city in cities:
                add_query(pool, f"{subject} {topic} in {city}")

    subjects_without_city = products + programming + food + entertainment
    long_tail_actions = [
        "guide", "tips", "ideas", "comparison", "alternatives", "checklist",
        "explained", "faq",
    ]
    long_tail_audiences = [
        "beginners", "students", "working professionals", "families", "travelers",
        "gamers", "developers", "daily users", "weekend users", "budget buyers",
    ]

    for subject in subjects_without_city:
        for action in long_tail_actions:
            for audience in long_tail_audiences:
                for city in cities:
                    for year in years:
                        add_query(pool, f"{subject} {action} for {audience} in {city} {year}")
                        add_query(pool, f"best {subject} {action} for {audience} in {city} {year}")

    return pool


def select_queries(pool: OrderedDict[str, str], rows: int, seed: int) -> list[str]:
    if rows <= 0:
        raise ValueError("rows must be a positive integer")

    mandatory = [
        pool[normalize_query(query)]
        for query in [
            "iphone",
            "java",
            "python",
            "laptop",
            "shoes",
            "pizza",
            "netflix",
            "cricket",
            "bangalore",
            "spring boot",
        ]
    ]

    remaining = [query for query in pool.values() if query not in mandatory]
    rng = random.Random(seed)
    rng.shuffle(remaining)

    selected = mandatory + remaining
    if rows > len(selected):
        raise ValueError(f"requested {rows} rows but only {len(selected)} unique queries are available")
    return selected[:rows]


def build_counts(rows: int, seed: int) -> list[int]:
    rng = random.Random(seed + 1000)
    counts = []
    for index in range(rows):
        if index < 50:
            count = rng.randint(50000, 250000)
        elif index < 5000:
            count = rng.randint(3000, 40000)
        elif index < 40000:
            count = rng.randint(200, 6000)
        else:
            count = rng.randint(1, 400)
        counts.append(count)
    counts.sort(reverse=True)
    return counts


def write_dataset(rows: int, output_path: Path, seed: int) -> dict[str, int]:
    pool = build_query_pool()
    selected_queries = select_queries(pool, rows, seed)
    counts = build_counts(rows, seed)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="", encoding="utf-8") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["query", "count"])
        for query, count in zip(selected_queries, counts):
            writer.writerow([query, count])

    return validate_dataset(output_path, rows)


def validate_dataset(csv_path: Path, expected_rows: int) -> dict[str, int]:
    if not csv_path.exists():
        raise FileNotFoundError(f"dataset file not found: {csv_path}")

    normalized_queries: set[str] = set()
    row_count = 0
    min_count = None
    max_count = None

    with csv_path.open("r", newline="", encoding="utf-8") as csv_file:
        reader = csv.reader(csv_file)
        try:
            header = next(reader)
        except StopIteration as exc:
            raise ValueError("dataset file is empty") from exc

        if header != ["query", "count"]:
            raise ValueError("header must be exactly: query,count")

        for row_number, row in enumerate(reader, start=2):
            if len(row) != 2:
                raise ValueError(f"row {row_number} must contain exactly 2 columns")

            query, count_text = row[0], row[1]
            normalized = normalize_query(query)
            if not normalized:
                raise ValueError(f"row {row_number} has an empty query")
            if normalized in normalized_queries:
                raise ValueError(f"row {row_number} duplicates normalized query: {normalized}")

            try:
                count = int(count_text)
            except ValueError as exc:
                raise ValueError(f"row {row_number} has a non-integer count: {count_text}") from exc

            if count <= 0:
                raise ValueError(f"row {row_number} has a non-positive count: {count}")

            normalized_queries.add(normalized)
            row_count += 1
            min_count = count if min_count is None else min(min_count, count)
            max_count = count if max_count is None else max(max_count, count)

    if row_count != expected_rows:
        raise ValueError(f"expected {expected_rows} data rows but found {row_count}")

    return {
        "rows_written": row_count,
        "unique_normalized_queries": len(normalized_queries),
        "min_count": min_count or 0,
        "max_count": max_count or 0,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate a synthetic search-query dataset.")
    parser.add_argument("--rows", type=int, default=100000, help="Number of data rows to generate.")
    parser.add_argument("--output", default="data/queries.csv", help="Output CSV path.")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for deterministic generation.")
    parser.add_argument(
        "--validate",
        action="store_true",
        help="Validate an existing dataset file instead of generating a new one.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_path = Path(args.output)

    if args.validate:
        summary = validate_dataset(output_path, args.rows)
    else:
        summary = write_dataset(args.rows, output_path, args.seed)

    print(f"output path: {output_path}")
    print(f"rows written: {summary['rows_written']}")
    print(f"unique normalized queries: {summary['unique_normalized_queries']}")
    print(f"min count: {summary['min_count']}")
    print(f"max count: {summary['max_count']}")


if __name__ == "__main__":
    main()
