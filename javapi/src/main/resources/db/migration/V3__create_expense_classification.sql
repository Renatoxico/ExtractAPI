CREATE TABLE public.expense_classification (
    id bigint GENERATED ALWAYS AS IDENTITY,
    expense_name character varying NOT NULL,
    category character varying,
    source character varying(16),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT expense_classification_pkey PRIMARY KEY (id),
    CONSTRAINT expense_classification_expense_name_key UNIQUE (expense_name),
    CONSTRAINT expense_classification_source_check
        CHECK (source IN ('AI', 'RULE', 'MANUAL'))
);
