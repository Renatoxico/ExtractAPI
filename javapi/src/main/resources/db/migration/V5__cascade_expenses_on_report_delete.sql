ALTER TABLE public.expense
    DROP CONSTRAINT expense_report_id_fkey;

ALTER TABLE public.expense
    ADD CONSTRAINT expense_report_id_fkey
    FOREIGN KEY (report_id) REFERENCES public.expense_report(id)
    ON DELETE CASCADE;
