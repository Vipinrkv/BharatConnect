import os
import re
import psycopg2

def main():
    # 1. Resolve paths relative to this script's directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.dirname(script_dir)
    
    env_path = os.path.join(workspace_root, "apps", "backend", ".env")
    if not os.path.exists(env_path):
        # Fallback to current directory check
        env_path = ".env"
        if not os.path.exists(env_path):
            print(f"Error: .env file not found")
            return

    db_url = None
    with open(env_path, "r") as f:
        for line in f:
            if line.strip().startswith("DATABASE_URL="):
                db_url = line.split("=", 1)[1].strip()
                break

    if not db_url:
        print("Error: DATABASE_URL not found in .env")
        return

    # Replace postgresql+asyncpg:// with postgresql:// for psycopg2 compatibility
    db_url = db_url.replace("postgresql+asyncpg://", "postgresql://")
    print(f"Connecting to database...")

    # 2. Connect to PostgreSQL
    try:
        conn = psycopg2.connect(db_url)
        conn.autocommit = True
        cur = conn.cursor()
        print("Connection successful!")
    except Exception as e:
        print(f"Failed to connect to database: {e}")
        return

    # 3. Find and sort migrations
    migrations_dir = os.path.join(workspace_root, "supabase", "migrations")
    if not os.path.exists(migrations_dir):
        print(f"Error: Migrations directory not found at {migrations_dir}")
        return

    migration_files = sorted([
        f for f in os.listdir(migrations_dir)
        if f.endswith(".sql")
    ])

    print(f"Found {len(migration_files)} migration files.")

    # 4. Execute migrations
    for filename in migration_files:
        filepath = os.path.join(migrations_dir, filename)
        print(f"Applying migration: {filename}...")
        with open(filepath, "r", encoding="utf-8") as f:
            sql_content = f.read()

        try:
            # We run each migration file
            cur.execute(sql_content)
            print(f"Successfully applied: {filename}")
        except Exception as e:
            print(f"Error executing migration {filename}: {e}")
            conn.rollback()
            return

    # 5. Execute seed data
    seed_path = os.path.join(workspace_root, "supabase", "seed", "seed.sql")
    if os.path.exists(seed_path):
        print(f"Applying seed data: {seed_path}...")
        with open(seed_path, "r", encoding="utf-8") as f:
            seed_content = f.read()
        try:
            cur.execute(seed_content)
            print("Successfully applied seed data!")
        except Exception as e:
            print(f"Error executing seed data: {e}")
            conn.rollback()
            return
    else:
        print("Seed file not found, skipping seed phase.")

    cur.close()
    conn.close()
    print("Database migrations applied successfully!")

if __name__ == "__main__":
    main()
