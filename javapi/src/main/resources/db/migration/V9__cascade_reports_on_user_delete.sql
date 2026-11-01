ALTER TABLE public.expense_report
    DROP CONSTRAINT expense_report_app_user_id_fkey;

ALTER TABLE public.expense_report
    ADD CONSTRAINT expense_report_app_user_id_fkey
    FOREIGN KEY (app_user_id) REFERENCES public.app_user(id)
    ON DELETE CASCADE;
