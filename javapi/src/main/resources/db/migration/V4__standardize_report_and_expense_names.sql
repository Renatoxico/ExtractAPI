DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.tb_expense expense
        LEFT JOIN public.expense_report report
            ON report.session_id = expense.session_id
        WHERE report.session_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot standardize report identifiers while orphan expenses exist';
    END IF;
END $$;

UPDATE public.tb_expense
SET transaction_type = NULL
WHERE transaction_type = '';

ALTER TABLE public.expense_report RENAME COLUMN session_id TO id;
ALTER TABLE public.expense_report RENAME COLUMN creation_date TO created_at;

ALTER TABLE public.tb_expense RENAME TO expense;
ALTER TABLE public.expense RENAME COLUMN session_id TO report_id;
ALTER TABLE public.expense RENAME COLUMN transaction_name TO expense_name;
ALTER TABLE public.expense RENAME COLUMN transaction_type TO category;
ALTER TABLE public.expense RENAME COLUMN value TO amount;

ALTER SEQUENCE public.tb_expense_id_seq RENAME TO expense_id_seq;

ALTER TABLE public.expense
    ADD CONSTRAINT expense_pkey PRIMARY KEY (id);

ALTER TABLE public.expense
    ALTER COLUMN report_id SET NOT NULL;

ALTER TABLE public.expense
    ADD CONSTRAINT expense_report_id_fkey
    FOREIGN KEY (report_id) REFERENCES public.expense_report(id);

CREATE INDEX idx_expense_report_id ON public.expense (report_id);

ALTER TABLE public.expense_report
    RENAME CONSTRAINT expense_report_pkey TO expense_report_id_pkey;
