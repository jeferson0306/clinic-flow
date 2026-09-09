-- Only the SHA-256 hash of a refresh token is ever stored — same reasoning
-- as a password hash, minus the deliberate slowness: this value is already
-- high-entropy random bytes, not something worth defending against a
-- dictionary attack, so a fast cryptographic hash is the right tool, not
-- bcrypt.
create table refresh_tokens (
    id          uuid primary key,
    user_id     uuid not null references users (id) on delete cascade,
    token_hash  text not null unique,
    expires_at  timestamptz not null,
    revoked_at  timestamptz,
    created_at  timestamptz not null
);

create index refresh_tokens_user_id_idx on refresh_tokens (user_id);
