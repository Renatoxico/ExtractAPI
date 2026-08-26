CREATE TABLE public.ai_classification_batch (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status character varying(16) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    input_payload jsonb NOT NULL,
    output_payload jsonb,
    last_error text,
    next_attempt_at timestamp with time zone,
    lease_expires_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    CONSTRAINT ai_classification_batch_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'RETRY', 'FAILED')),
    CONSTRAINT ai_classification_batch_attempts_check CHECK (attempts >= 0)
);

CREATE TABLE public.expense_classification_task (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id bigint NOT NULL,
    classification_id bigint NOT NULL,
    suggested_category character varying,
    status character varying(24) NOT NULL DEFAULT 'PENDING_AI',
    apply_attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp with time zone,
    last_error text,
    lease_expires_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at timestamp with time zone,
    applied_at timestamp with time zone,
    finished_at timestamp with time zone,
    CONSTRAINT expense_classification_task_batch_fkey
        FOREIGN KEY (batch_id) REFERENCES public.ai_classification_batch(id),
    CONSTRAINT expense_classification_task_classification_fkey
        FOREIGN KEY (classification_id) REFERENCES public.expense_classification(id) ON DELETE CASCADE,
    CONSTRAINT expense_classification_task_status_check
        CHECK (status IN (
            'PENDING_AI', 'READY_TO_APPLY', 'APPLYING', 'APPLIED', 'RETRY', 'FAILED'
        )),
    CONSTRAINT expense_classification_task_apply_attempts_check CHECK (apply_attempts >= 0)
);

CREATE UNIQUE INDEX idx_expense_classification_task_one_active
    ON public.expense_classification_task (classification_id)
    WHERE status NOT IN ('APPLIED', 'FAILED');

CREATE INDEX idx_expense_classification_task_history
    ON public.expense_classification_task (classification_id, created_at, id);

CREATE INDEX idx_ai_classification_batch_eligible
    ON public.ai_classification_batch (status, next_attempt_at, id)
    WHERE status IN ('PENDING', 'RETRY');

CREATE INDEX idx_ai_classification_batch_lease
    ON public.ai_classification_batch (lease_expires_at, id)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_expense_classification_task_batch_waiting
    ON public.expense_classification_task (batch_id, id)
    WHERE status = 'PENDING_AI';

CREATE INDEX idx_expense_classification_task_apply_eligible
    ON public.expense_classification_task (status, next_attempt_at, id)
    WHERE status IN ('READY_TO_APPLY', 'RETRY') AND suggested_category IS NOT NULL;

CREATE INDEX idx_expense_classification_task_lease
    ON public.expense_classification_task (lease_expires_at, id)
    WHERE status = 'APPLYING';

DO $$
DECLARE
    new_batch_id bigint;
    inserted_tasks integer;
    has_candidates boolean;
BEGIN
    LOOP
        SELECT EXISTS (
            SELECT 1
            FROM public.expense_classification classification
            WHERE NULLIF(classification.category, '') IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.expense_classification_task task
                  WHERE task.classification_id = classification.id
                    AND task.status NOT IN ('APPLIED', 'FAILED')
              )
        ) INTO has_candidates;

        EXIT WHEN NOT has_candidates;

        INSERT INTO public.ai_classification_batch (status, input_payload)
        VALUES ('PENDING', '{"schemaVersion":1,"items":[]}'::jsonb)
        RETURNING id INTO new_batch_id;

        INSERT INTO public.expense_classification_task (batch_id, classification_id, status)
        SELECT new_batch_id, classification.id, 'PENDING_AI'
        FROM public.expense_classification classification
        WHERE NULLIF(classification.category, '') IS NULL
          AND NOT EXISTS (
              SELECT 1
              FROM public.expense_classification_task task
              WHERE task.classification_id = classification.id
                AND task.status NOT IN ('APPLIED', 'FAILED')
          )
        ORDER BY classification.id
        LIMIT 50;

        GET DIAGNOSTICS inserted_tasks = ROW_COUNT;
        IF inserted_tasks = 0 THEN
            RAISE EXCEPTION 'Could not create tasks for classification backfill batch %', new_batch_id;
        END IF;

        UPDATE public.ai_classification_batch batch
        SET input_payload = (
            SELECT jsonb_build_object(
                'schemaVersion', 1,
                'items', jsonb_agg(
                    jsonb_build_object(
                        'taskId', task.id,
                        'expenseName', classification.expense_name
                    ) ORDER BY task.id
                )
            )
            FROM public.expense_classification_task task
            JOIN public.expense_classification classification
              ON classification.id = task.classification_id
            WHERE task.batch_id = new_batch_id
        )
        WHERE batch.id = new_batch_id;
    END LOOP;
END
$$;
