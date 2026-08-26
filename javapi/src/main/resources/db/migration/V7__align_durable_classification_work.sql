ALTER TABLE public.expense_classification_task
    ADD COLUMN IF NOT EXISTS finished_at timestamp with time zone;

UPDATE public.expense_classification_task
SET status = 'PENDING_AI',
    lease_expires_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'PROCESSING_AI';

DO $$
DECLARE
    orphan_batch_id bigint;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.expense_classification_task
        WHERE batch_id IS NULL
    ) THEN
        INSERT INTO public.ai_classification_batch (
            status,
            input_payload,
            last_error,
            finished_at
        )
        VALUES (
            'FAILED',
            '{"schemaVersion":1,"items":[]}'::jsonb,
            'Migration V7 placeholder batch for tasks orphaned by old V6 ON DELETE SET NULL',
            CURRENT_TIMESTAMP
        )
        RETURNING id INTO orphan_batch_id;

        UPDATE public.expense_classification_task
        SET batch_id = orphan_batch_id,
            status = 'FAILED',
            last_error = COALESCE(
                last_error,
                'Task orphaned by old V6 batch deletion behavior before V7 migration'
            ),
            next_attempt_at = NULL,
            lease_expires_at = NULL,
            finished_at = COALESCE(finished_at, CURRENT_TIMESTAMP),
            updated_at = CURRENT_TIMESTAMP
        WHERE batch_id IS NULL;
    END IF;
END
$$;

ALTER TABLE public.expense_classification_task
    DROP CONSTRAINT IF EXISTS expense_classification_task_classification_key,
    DROP CONSTRAINT IF EXISTS expense_classification_task_ai_attempts_check,
    DROP CONSTRAINT IF EXISTS expense_classification_task_batch_fkey,
    DROP CONSTRAINT IF EXISTS expense_classification_task_status_check;

DROP INDEX IF EXISTS public.idx_expense_classification_task_ai_eligible;
DROP INDEX IF EXISTS public.idx_expense_classification_task_lease;
DROP INDEX IF EXISTS public.expense_classification_task_classification_key;

ALTER TABLE public.expense_classification_task
    DROP COLUMN IF EXISTS ai_attempts;

ALTER TABLE public.expense_classification_task
    ALTER COLUMN batch_id SET NOT NULL;

ALTER TABLE public.expense_classification_task
    ADD CONSTRAINT expense_classification_task_batch_fkey
        FOREIGN KEY (batch_id) REFERENCES public.ai_classification_batch(id),
    ADD CONSTRAINT expense_classification_task_status_check
        CHECK (status IN (
            'PENDING_AI', 'READY_TO_APPLY', 'APPLYING', 'APPLIED', 'RETRY', 'FAILED'
        ));

CREATE UNIQUE INDEX IF NOT EXISTS idx_expense_classification_task_one_active
    ON public.expense_classification_task (classification_id)
    WHERE status NOT IN ('APPLIED', 'FAILED');

CREATE INDEX IF NOT EXISTS idx_expense_classification_task_history
    ON public.expense_classification_task (classification_id, created_at, id);

CREATE INDEX IF NOT EXISTS idx_expense_classification_task_batch_waiting
    ON public.expense_classification_task (batch_id, id)
    WHERE status = 'PENDING_AI';

CREATE INDEX IF NOT EXISTS idx_expense_classification_task_lease
    ON public.expense_classification_task (lease_expires_at, id)
    WHERE status = 'APPLYING';
