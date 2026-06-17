-- BharatConnect Development Seed Script

-- 1. Create Mock Auth Users (Supabase Auth Schema)
-- Disable trigger temporarily to seed profiles and auth users manually without key mismatches
ALTER TABLE auth.users DISABLE TRIGGER on_auth_user_created;

INSERT INTO auth.users (id, phone, email, raw_user_meta_data, created_at, updated_at, aud, role)
VALUES 
  ('a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', '+919876543210', 'arjun.sharma@gmail.com', '{"display_name": "Arjun Sharma"}', NOW(), NOW(), 'authenticated', 'authenticated'),
  ('b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', '+919999999999', 'ritu.volunteer@gmail.com', '{"display_name": "Volunteer Ritu"}', NOW(), NOW(), 'authenticated', 'authenticated'),
  ('c3d4e5f6-a7b8-91c2-d3e4-f5a6b7c8d9e0', '+918888888888', 'amit.admin@gmail.com', '{"display_name": "Admin Amit"}', NOW(), NOW(), 'authenticated', 'authenticated')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE auth.users ENABLE TRIGGER on_auth_user_created;

-- 2. Insert Matching Public Profiles (Delhi coordinates)
INSERT INTO public.profiles (id, phone, display_name, avatar_url, location_coordinates, location_updated_at, is_verified_helper, helper_trust_score)
VALUES
  ('a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', '+919876543210', 'Arjun Sharma', 'https://api.dicebear.com/7.x/bottts/svg?seed=arjun', ST_SetSRID(ST_Point(77.2090, 28.6139), 4326)::geography, NOW(), false, 5.00),
  ('b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', '+919999999999', 'Volunteer Ritu', 'https://api.dicebear.com/7.x/bottts/svg?seed=ritu', ST_SetSRID(ST_Point(77.2182, 28.6250), 4326)::geography, NOW(), true, 4.90),
  ('c3d4e5f6-a7b8-91c2-d3e4-f5a6b7c8d9e0', '+918888888888', 'Admin Amit', 'https://api.dicebear.com/7.x/bottts/svg?seed=amit', ST_SetSRID(ST_Point(77.2000, 28.6000), 4326)::geography, NOW(), true, 4.50)
ON CONFLICT (id) DO UPDATE 
SET display_name = EXCLUDED.display_name, is_verified_helper = EXCLUDED.is_verified_helper, helper_trust_score = EXCLUDED.helper_trust_score, location_coordinates = EXCLUDED.location_coordinates;

-- 3. Seed Conversations (Chats)
INSERT INTO public.chats (id, type, title, avatar_url)
VALUES 
  ('8b51fe44-42b7-4c4f-a035-64506c117d91', 'direct', NULL, NULL),
  ('9b51fe44-42b7-4c4f-a035-64506c117d92', 'group', 'Delhi Tech Volunteer Group', 'https://api.dicebear.com/7.x/identicon/svg?seed=delhi-group')
ON CONFLICT (id) DO NOTHING;

-- 4. Seed Membership Links
INSERT INTO public.chat_members (chat_id, profile_id, role)
VALUES
  ('8b51fe44-42b7-4c4f-a035-64506c117d91', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', 'member'),
  ('8b51fe44-42b7-4c4f-a035-64506c117d91', 'b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', 'member'),
  ('9b51fe44-42b7-4c4f-a035-64506c117d92', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', 'member'),
  ('9b51fe44-42b7-4c4f-a035-64506c117d92', 'b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', 'member'),
  ('9b51fe44-42b7-4c4f-a035-64506c117d92', 'c3d4e5f6-a7b8-91c2-d3e4-f5a6b7c8d9e0', 'admin')
ON CONFLICT (chat_id, profile_id) DO NOTHING;

-- 5. Seed Messages
INSERT INTO public.messages (id, chat_id, sender_id, content_type, text_content, created_at)
VALUES 
  ('c7a72d38-9cb5-46aa-b2b9-7b3b3a62886f', '8b51fe44-42b7-4c4f-a035-64506c117d91', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', 'text', 'Hello, are you available?', NOW() - INTERVAL '5 minutes'),
  ('d7a72d38-9cb5-46aa-b2b9-7b3b3a62887f', '8b51fe44-42b7-4c4f-a035-64506c117d91', 'b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', 'text', 'Yes, online now. Did the files upload correctly?', NOW() - INTERVAL '2 minutes'),
  ('e7a72d38-9cb5-46aa-b2b9-7b3b3a62888f', '9b51fe44-42b7-4c4f-a035-64506c117d92', 'b2f3d4c5-e6a7-48b9-91c2-c3d4e5f6a7b8', 'text', 'I have picked up the emergency medicine dispatch request. Navigating to recipient location now.', NOW() - INTERVAL '10 minutes'),
  ('f7a72d38-9cb5-46aa-b2b9-7b3b3a62889f', '9b51fe44-42b7-4c4f-a035-64506c117d92', 'c3d4e5f6-a7b8-91c2-d3e4-f5a6b7c8d9e0', 'text', 'Excellent, Ritu. Keep us posted. Coordination is fully stored in local SQLite cache.', NOW() - INTERVAL '8 minutes')
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Help Requests (Verified Help - SOS Board)
INSERT INTO public.help_requests (id, requester_id, title, description, category, location, status, min_trust_score)
VALUES 
  ('1b51fe44-42b7-4c4f-a035-64506c117d93', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', '🔴 EMERGENCY: Medical Dispatch', 'Need insulin delivery for senior citizen at Pocket A-4, Janakpuri. Requires trust score 4.5+.', 'Medical', ST_SetSRID(ST_Point(77.2000, 28.6100), 4326)::geography, 'open', 4.50),
  ('2b51fe44-42b7-4c4f-a035-64506c117d94', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', 'Water Logger Rescue', 'Need volunteers with towing vehicle to clear main junction block after heavy rains.', 'Disaster Support', ST_SetSRID(ST_Point(77.2200, 28.6300), 4326)::geography, 'open', 3.00)
ON CONFLICT (id) DO NOTHING;

-- 7. Seed Need It Now requests (Marketplace Gigs)
INSERT INTO public.need_it_now_requests (id, requester_id, title, description, budget_estimate, location, status)
VALUES
  ('3b51fe44-42b7-4c4f-a035-64506c117d95', 'a4d3f572-dcd6-43e7-910a-b28ccf5d6f12', '🔧 Plumber Needed Immediately', 'Kitchen sink pipe leaking heavily. Water damage starting. Need someone who can visit within 1 hour.', 500.00, ST_SetSRID(ST_Point(77.2050, 28.6150), 4326)::geography, 'active')
ON CONFLICT (id) DO NOTHING;
