-- Database Migration: Need It Now Hyperlocal Gig Marketplace Upgrade

-- 1. Extend Need It Now Requests Table with category, expires_at, and resolution links
ALTER TABLE public.need_it_now_requests
  ADD COLUMN IF NOT EXISTS category VARCHAR(30) CHECK (category IN ('Emergency', 'Transport', 'Borrow', 'Tools', 'Tickets', 'Services')),
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '2 hours') NOT NULL,
  ADD COLUMN IF NOT EXISTS accepted_bid_id UUID;

-- 2. Extend Need It Now Bids Table with chat integration
ALTER TABLE public.need_it_now_bids
  ADD COLUMN IF NOT EXISTS chat_id UUID REFERENCES public.chats(id) ON DELETE SET NULL;

-- Bind accepted_bid_id Foreign Key
ALTER TABLE public.need_it_now_requests
  ADD CONSTRAINT fk_requests_accepted_bid
  FOREIGN KEY (accepted_bid_id) REFERENCES public.need_it_now_bids(id) ON DELETE SET NULL;

-- 3. Trigger Function to automatically reject other bids when one is accepted
CREATE OR REPLACE FUNCTION public.handle_bid_acceptance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'accepted' THEN
        -- Reject all other bids for the same request
        UPDATE public.need_it_now_bids
        SET status = 'rejected'
        WHERE request_id = NEW.request_id AND id <> NEW.id;
        
        -- Update the request status to 'fulfilled'
        UPDATE public.need_it_now_requests
        SET status = 'fulfilled',
            accepted_bid_id = NEW.id
        WHERE id = NEW.request_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_bid_accepted
  AFTER UPDATE OF status ON public.need_it_now_bids
  FOR EACH ROW
  WHEN (NEW.status = 'accepted' AND OLD.status <> 'accepted')
  EXECUTE FUNCTION public.handle_bid_acceptance();
