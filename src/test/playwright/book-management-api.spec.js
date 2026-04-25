// LIB-15: Audit Trail   — createdAt / updatedAt populated automatically
// LIB-4:  Book Status   — defaults to AVAILABLE, can be set to RESERVED
// LIB-13: Input Validation — 400 on invalid body, 404 on unknown id, 409 on duplicate ISBN, 201 on success

const { test, expect } = require('@playwright/test');

// ── Helpers ───────────────────────────────────────────────────────────────────

async function createBook(request, overrides = {}) {
  return request.post('/api/books', {
    data: {
      title: 'Default Title',
      author: 'Default Author',
      isbn: '978-0000000001',
      genre: 'FICTION',
      publishedYear: 2000,
      ...overrides
    }
  });
}

// ── LIB-15: Audit trail ───────────────────────────────────────────────────────

test.describe('[LIB-15] Audit trail', () => {
  test('createdAt and updatedAt are populated automatically on book creation', async ({ request }) => {
    const response = await createBook(request, {
      title: 'Clean Code',
      author: 'Robert C. Martin',
      isbn: '978-0132350884',
      genre: 'TECHNOLOGY',
      publishedYear: 2008
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.createdAt).not.toBeNull();
    expect(body.updatedAt).not.toBeNull();
    expect(body.createdAt).toEqual(body.updatedAt);
  });
});

// ── LIB-4: Book status ────────────────────────────────────────────────────────

test.describe('[LIB-4] Book status', () => {
  test('status defaults to AVAILABLE on creation', async ({ request }) => {
    const response = await createBook(request, {
      title: 'The Pragmatic Programmer',
      author: 'David Thomas',
      isbn: '978-0135957059',
      genre: 'TECHNOLOGY',
      publishedYear: 1999
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.status).toBe('AVAILABLE');
  });

  test('status can be set to RESERVED via POST /api/books/{id}/reserve', async ({ request }) => {
    // Create a book first
    const createResponse = await createBook(request, {
      title: 'Effective Java',
      author: 'Joshua Bloch',
      isbn: '978-0134685991',
      genre: 'TECHNOLOGY',
      publishedYear: 2018
    });
    expect(createResponse.status()).toBe(201);
    const { id } = await createResponse.json();

    // Reserve it
    const reserveResponse = await request.post(`/api/books/${id}/reserve`);
    expect(reserveResponse.status()).toBe(200);
    const body = await reserveResponse.json();
    expect(body.status).toBe('RESERVED');
  });
});

// ── LIB-13: Input validation ──────────────────────────────────────────────────

test.describe('[LIB-13] Input validation', () => {
  test('POST /api/books with a missing required field returns 400', async ({ request }) => {
    const response = await request.post('/api/books', {
      data: {
        // title intentionally omitted
        author: 'Unknown Author',
        isbn: '978-0000000010',
        genre: 'FICTION',
        publishedYear: 2000
      }
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.error).toBe('Bad Request');
    expect(body.message).toBe('Validation failed');
    expect(body.validationErrors?.title).toBe('Title is required');
  });

  test('POST /api/books with a malformed JSON body returns 400', async ({ request }) => {
    const response = await request.post('/api/books', {
      headers: { 'Content-Type': 'application/json' },
      data: '{ invalid json }'
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.message).toBe('Request body is invalid or malformed');
  });

  test('POST /api/books with an ISBN failing the pattern constraint returns 400', async ({ request }) => {
    const response = await request.post('/api/books', {
      data: {
        title: 'Bad ISBN Book',
        author: 'Some Author',
        isbn: 'BADISBN!!!',
        genre: 'FICTION',
        publishedYear: 2000
      }
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.validationErrors?.isbn).toBe(
      'ISBN must be 10 to 17 characters and contain only digits, X, or hyphen'
    );
  });

  test('GET /api/books/{id} for an unknown id returns 404', async ({ request }) => {
    const response = await request.get('/api/books/99999');

    expect(response.status()).toBe(404);
    const body = await response.json();
    expect(body.error).toBe('Not Found');
  });

  test('POST /api/books with a duplicate ISBN returns 409', async ({ request }) => {
    const bookData = {
      title: 'Domain-Driven Design',
      author: 'Eric Evans',
      isbn: '978-0321125217',
      genre: 'TECHNOLOGY',
      publishedYear: 2003
    };

    // First creation succeeds
    const first = await createBook(request, bookData);
    expect(first.status()).toBe(201);

    // Second creation with same ISBN → 409
    const second = await createBook(request, { ...bookData, title: 'Another Book' });
    expect(second.status()).toBe(409);
    const body = await second.json();
    expect(body.error).toBe('Conflict');
    expect(body.message).toContain('978-0321125217');
  });

  test('POST /api/books with a valid body returns 201 with the created book', async ({ request }) => {
    const response = await createBook(request, {
      title: 'Domain-Driven Design',
      author: 'Eric Evans',
      isbn: '978-0321125218',
      genre: 'TECHNOLOGY',
      publishedYear: 2003
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.title).toBe('Domain-Driven Design');
    expect(body.author).toBe('Eric Evans');
    expect(body.isbn).toBe('978-0321125218');
    expect(body.status).toBe('AVAILABLE');
    expect(body.id).not.toBeNull();
    expect(body.createdAt).not.toBeNull();
    expect(body.updatedAt).not.toBeNull();
  });
});
