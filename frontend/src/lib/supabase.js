import { createClient } from "@supabase/supabase-js";

const runtimeConfig = window.__FINDLOOP_CONFIG__ || {};
const supabaseUrl = runtimeConfig.supabaseUrl || import.meta.env.VITE_SUPABASE_URL;
const supabasePublishableKey =
    runtimeConfig.supabasePublishableKey || import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;

export const supabase = createClient(
    supabaseUrl,
    supabasePublishableKey
);
