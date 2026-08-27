CREATE TABLE public.admin_email_outbox (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_type character varying(32) NOT NULL,
    deduplication_key character varying(255) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(16) NOT NULL DEFAULT 'PENDING',
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp with time zone,
    lease_expires_at timestamp with time zone,
    last_error text,
    original_notification_id bigint,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at timestamp with time zone,
    CONSTRAINT admin_email_outbox_deduplication_key_key UNIQUE (deduplication_key),
    CONSTRAINT admin_email_outbox_original_notification_fkey
        FOREIGN KEY (original_notification_id) REFERENCES public.admin_email_outbox(id),
    CONSTRAINT admin_email_outbox_status_check
        CHECK (status IN ('PENDING', 'SENDING', 'RETRY', 'SENT', 'FAILED')),
    CONSTRAINT admin_email_outbox_attempts_check CHECK (attempts >= 0)
);

CREATE INDEX idx_admin_email_outbox_eligible
    ON public.admin_email_outbox (status, next_attempt_at, id)
    WHERE status IN ('PENDING', 'RETRY');

CREATE INDEX idx_admin_email_outbox_lease
    ON public.admin_email_outbox (lease_expires_at, id)
    WHERE status = 'SENDING';
