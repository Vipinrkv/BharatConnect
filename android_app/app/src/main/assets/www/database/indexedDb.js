/**
 * BharatConnect Structured IndexedDB Engine (www/database/indexedDb.js)
 * 
 * Asynchronous, non-blocking IndexedDB persistence layer replacing LocalStorage
 * for high-speed local data storage, multi-index querying, and offline sync queuing.
 */

class BharatConnectIndexedDB {
    constructor(dbName = "BharatConnectLocalDB", version = 1) {
        this.dbName = dbName;
        this.version = version;
        this.db = null;
        this._initPromise = null;
    }

    async init() {
        if (this.db) return this.db;
        if (this._initPromise) return this._initPromise;

        this._initPromise = new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.version);

            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                console.log("[IndexedDB] Upgrading schema version to:", this.version);

                // 1. Users Store
                if (!db.objectStoreNames.contains("users")) {
                    const userStore = db.createObjectStore("users", { keyPath: "id" });
                    userStore.createIndex("username", "username", { unique: false });
                    userStore.createIndex("phone", "phone", { unique: false });
                }

                // 2. Conversations Store
                if (!db.objectStoreNames.contains("conversations")) {
                    const convStore = db.createObjectStore("conversations", { keyPath: "id" });
                    convStore.createIndex("type", "type", { unique: false });
                    convStore.createIndex("updated_at", "updated_at", { unique: false });
                }

                // 3. Messages Store
                if (!db.objectStoreNames.contains("messages")) {
                    const msgStore = db.createObjectStore("messages", { keyPath: "id" });
                    msgStore.createIndex("conversation_id", "conversation_id", { unique: false });
                    msgStore.createIndex("sequence", "sequence", { unique: false });
                    msgStore.createIndex("client_message_id", "client_message_id", { unique: true });
                    msgStore.createIndex("created_at", "created_at", { unique: false });
                }

                // 4. Posts Store
                if (!db.objectStoreNames.contains("posts")) {
                    const postStore = db.createObjectStore("posts", { keyPath: "id" });
                    postStore.createIndex("created_at", "created_at", { unique: false });
                }

                // 5. Offline Sync Queue Store
                if (!db.objectStoreNames.contains("sync_queue")) {
                    const syncStore = db.createObjectStore("sync_queue", { keyPath: "id" });
                    syncStore.createIndex("status", "status", { unique: false });
                    syncStore.createIndex("created_at", "created_at", { unique: false });
                }

                // 6. App State / Settings Store
                if (!db.objectStoreNames.contains("app_state")) {
                    db.createObjectStore("app_state", { keyPath: "key" });
                }
            };

            request.onsuccess = (event) => {
                this.db = event.target.result;
                console.log("[IndexedDB] Database initialized successfully.");
                resolve(this.db);
            };

            request.onerror = (event) => {
                console.error("[IndexedDB] Database failed to open:", event.target.error);
                reject(event.target.error);
            };
        });

        return this._initPromise;
    }

    async get(storeName, key) {
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readonly");
            const store = transaction.objectStore(storeName);
            const request = store.get(key);

            request.onsuccess = () => resolve(request.result || null);
            request.onerror = () => reject(request.error);
        });
    }

    async getAll(storeName, indexName = null, query = null) {
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readonly");
            const store = transaction.objectStore(storeName);
            const target = indexName ? store.index(indexName) : store;
            const request = target.getAll(query);

            request.onsuccess = () => resolve(request.result || []);
            request.onerror = () => reject(request.error);
        });
    }

    async put(storeName, value) {
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readwrite");
            const store = transaction.objectStore(storeName);
            const request = store.put(value);

            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async putMany(storeName, values) {
        if (!values || values.length === 0) return;
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readwrite");
            const store = transaction.objectStore(storeName);

            values.forEach(val => store.put(val));

            transaction.oncomplete = () => resolve(true);
            transaction.onerror = () => reject(transaction.error);
        });
    }

    async delete(storeName, key) {
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readwrite");
            const store = transaction.objectStore(storeName);
            const request = store.delete(key);

            request.onsuccess = () => resolve(true);
            request.onerror = () => reject(request.error);
        });
    }

    async clear(storeName) {
        const db = await this.init();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, "readwrite");
            const store = transaction.objectStore(storeName);
            const request = store.clear();

            request.onsuccess = () => resolve(true);
            request.onerror = () => reject(request.error);
        });
    }
}

window.bharatConnectIDB = new BharatConnectIndexedDB();
