-- Complete production schema immediately before Flyway adoption.
-- Source archive SHA-256:
-- 25E8B1F847AD2F8DB117A4A543E029227BB70B5088D149630F196CC4444D9254

CREATE TABLE public.app_user (
    id bigint NOT NULL,
    firebase_uid character varying(128) NOT NULL,
    email character varying(320),
    display_name character varying(255),
    picture_url character varying(2048)
);

CREATE SEQUENCE public.app_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.app_user_id_seq OWNED BY public.app_user.id;

CREATE TABLE public.expense_report (
    session_id character varying(32) NOT NULL,
    app_user_id bigint NOT NULL
);

CREATE TABLE public.tb_expense (
    value numeric,
    id bigint NOT NULL,
    date character varying,
    session_id character varying,
    transaction_name character varying,
    transaction_type character varying
);

ALTER TABLE public.tb_expense ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_expense_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(255),
    is_premium boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY public.app_user
    ALTER COLUMN id SET DEFAULT nextval('public.app_user_id_seq'::regclass);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_firebase_uid_key UNIQUE (firebase_uid);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expense_report
    ADD CONSTRAINT expense_report_pkey PRIMARY KEY (session_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_expense_report_app_user_id
    ON public.expense_report USING btree (app_user_id);

ALTER TABLE ONLY public.expense_report
    ADD CONSTRAINT expense_report_app_user_id_fkey
    FOREIGN KEY (app_user_id) REFERENCES public.app_user(id);
