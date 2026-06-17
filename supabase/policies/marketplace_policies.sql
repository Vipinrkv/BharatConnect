-- RLS Policies for Need It Now Gigs & Local Bids

-- Enable Row Level Security
ALTER TABLE public.need_it_now_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.need_it_now_bids ENABLE ROW LEVEL SECURITY;

-- 1. Gig Requests - Select Policy
-- Authenticated users can list all active gig demands.
CREATE POLICY select_gigs ON public.need_it_now_requests 
    FOR SELECT 
    TO authenticated 
    USING (true);

-- 2. Gig Requests - Insert Policy
-- Users can list new gig demands, binding their profile.
CREATE POLICY insert_gig ON public.need_it_now_requests 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (requester_id = auth.uid());

-- 3. Gig Requests - Update Policy
-- Requesters can edit or resolve their active gig demands.
CREATE POLICY update_own_gig ON public.need_it_now_requests 
    FOR UPDATE 
    TO authenticated 
    USING (requester_id = auth.uid());

-- 4. Bids - Select Policy
-- Bids are visible only to the bidder or the creator of the gig demand.
CREATE POLICY select_bids ON public.need_it_now_bids 
    FOR SELECT 
    TO authenticated
    USING (
        bidder_id = auth.uid() 
        OR EXISTS (
            SELECT 1 FROM public.need_it_now_requests r 
            WHERE r.id = need_it_now_bids.request_id 
            AND r.requester_id = auth.uid()
        )
    );

-- 5. Bids - Insert Policy
-- Providers can submit service bids, binding their profile.
CREATE POLICY insert_own_bid ON public.need_it_now_bids 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (bidder_id = auth.uid());

-- 6. Bids - Update Policy
-- Bidders can update their pricing or message parameters.
CREATE POLICY update_own_bid ON public.need_it_now_bids 
    FOR UPDATE 
    TO authenticated 
    USING (bidder_id = auth.uid());
