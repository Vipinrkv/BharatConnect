import { UserProfile, Chat, NearbyPost, HelpRequest } from '../types';

export const CURRENT_USER: UserProfile = {
  id: 'me',
  name: 'Alex Rivera',
  username: '@alexrivera',
  avatar: 'AR',
  avatarBg: 'bg-emerald-500',
  status: 'online',
  customStatus: '⚡ Building the ultimate UI',
  bio: 'Android-First designer & emergency rescue volunteer. Pixel enthusiast.',
  role: 'admin',
  location: 'Downtown District',
};

export const MOCK_USERS: UserProfile[] = [
  {
    id: 'user_1',
    name: 'Dr. Sophia Patel',
    username: '@sophiamed',
    avatar: 'SP',
    avatarBg: 'bg-rose-500 border border-red-400',
    status: 'online',
    customStatus: '🚑 Active responder - Keep line clear',
    bio: 'Emergency medicine clinician & community first-aider. Verified NGO partner.',
    role: 'verified_responder',
    location: 'Central Medical Block (400m away)',
    distance: '400m away',
  },
  {
    id: 'user_2',
    name: 'Marcus Chen',
    username: '@marcus_c',
    avatar: 'MC',
    avatarBg: 'bg-blue-500',
    status: 'idle',
    customStatus: '☕ Coding & Drinking Tea',
    bio: 'Software developer and food security organizer. Tech-for-good advocate.',
    role: 'member',
    location: 'Westside Heights (1.2km away)',
    distance: '1.2km away',
  },
  {
    id: 'user_3',
    name: 'Elena Rostova',
    username: '@elenarostov',
    avatar: 'ER',
    avatarBg: 'bg-amber-500',
    status: 'dnd',
    customStatus: '🔇 Do not disturb - sleeping',
    bio: 'Telegram Channel Administrator. Coordinates local volunteer grids.',
    role: 'moderator',
    location: 'East Side Plaza (800m away)',
    distance: '800m away',
  },
  {
    id: 'user_4',
    name: 'Liam Vance',
    username: '@liamvance',
    avatar: 'LV',
    avatarBg: 'bg-indigo-500',
    status: 'offline',
    customStatus: '💤 Gone fishing',
    bio: 'Power & utility technician. Let me know if grid services trip.',
    role: 'member',
    location: 'Substation Area (2.5km away)',
    distance: '2.5km away',
  },
  {
    id: 'user_5',
    name: 'Emergency Broadcast Channel',
    username: '@official_alerts',
    avatar: '🔔',
    avatarBg: 'bg-neutral-800 border-2 border-amber-500',
    status: 'online',
    customStatus: '📣 Official Verified Channel broadcasts',
    bio: 'Consolidated city civil defense feed for immediate response operations.',
    role: 'admin',
    location: 'City Operations Command',
    distance: 'Admin',
  },
  {
    id: 'user_6',
    name: 'Rescue Volunteers Grid',
    username: '@rescue_grid',
    avatar: '👥',
    avatarBg: 'bg-purple-600',
    status: 'online',
    customStatus: '🤝 42 volunteers online',
    bio: 'Collaborative volunteer channel mapping nearby emergency needs.',
    role: 'moderator',
    location: 'City Center',
    distance: 'Group',
  }
];

export const INITIAL_CHATS: Chat[] = [
  {
    id: 'chat_official',
    user: MOCK_USERS[4], // Emergency broadcast
    lastMessage: '🚩 Flood warning extended for South River side until 21:00. Use elevated flyovers.',
    unreadCount: 1,
    timestamp: '11:42 AM',
    isGroup: false,
    isChannel: true,
    category: 'channel',
    messages: [
      {
        id: 'msg_o1',
        senderId: 'user_5',
        text: '🚦 Traffic advisory: Central bridge under maintenance. Detours set in place.',
        timestamp: 'Yesterday',
        status: 'read'
      },
      {
        id: 'msg_o2',
        senderId: 'user_5',
        text: '🚩 Flood warning extended for South River side until 21:00. Use elevated flyovers.',
        timestamp: '11:42 AM',
        status: 'delivered'
      }
    ]
  },
  {
    id: 'chat_sophia',
    user: MOCK_USERS[0], // Dr. Sophia
    lastMessage: 'I have checked the insulin status. Preparing 5 vials for verified pick up.',
    unreadCount: 0,
    timestamp: '11:30 AM',
    isGroup: false,
    isChannel: false,
    category: 'secret',
    messages: [
      {
        id: 'msg1',
        senderId: 'me',
        text: 'Hello Dr. Sophia, is the insulin shipment verified for the Westside clinic?',
        timestamp: '11:15 AM',
        status: 'read'
      },
      {
        id: 'msg2',
        senderId: 'user_1',
        text: 'Yes! I have checked the insulin status. Preparing 5 vials for verified pick up.',
        timestamp: '11:30 AM',
        status: 'read'
      }
    ]
  },
  {
    id: 'chat_group_rescue',
    user: MOCK_USERS[5], // Group
    lastMessage: 'Marcus: Ready with the pickup truck. Anyone nearby to load the blankets?',
    unreadCount: 3,
    timestamp: '10:15 AM',
    isGroup: true,
    isChannel: false,
    category: 'group',
    messages: [
      {
        id: 'msg_g1',
        senderId: 'user_3',
        text: 'We got 50 emergency thermal blankets from the central registry.',
        timestamp: '09:50 AM',
        status: 'read'
      },
      {
        id: 'msg_g2',
        senderId: 'user_2',
        text: 'Ready with the pickup truck. Anyone nearby to load the blankets?',
        timestamp: '10:15 AM',
        status: 'read'
      }
    ]
  },
  {
    id: 'chat_marcus',
    user: MOCK_USERS[1], // Marcus
    lastMessage: 'Thanks for sending the schematic. I am heading to the grid node.',
    unreadCount: 0,
    timestamp: '09:05 AM',
    isGroup: false,
    isChannel: false,
    category: 'direct',
    messages: [
      {
        id: 'msg_m1',
        senderId: 'me',
        text: 'Hey Marcus, I uploaded the power station layout PDF to the secure box.',
        timestamp: '08:45 AM',
        status: 'read'
      },
      {
        id: 'msg_m2',
        senderId: 'user_2',
        text: 'Super helpful. Will check it shortly!',
        timestamp: '08:48 AM',
        status: 'read'
      },
      {
        id: 'msg_m3',
        senderId: 'user_2',
        text: 'Thanks for sending the schematic. I am heading to the grid node.',
        timestamp: '09:05 AM',
        status: 'read'
      }
    ]
  },
  {
    id: 'chat_elena',
    user: MOCK_USERS[2], // Elena
    lastMessage: 'Are we launching the verified volunteers check system tonight?',
    unreadCount: 0,
    timestamp: 'Yesterday',
    isGroup: false,
    isChannel: false,
    category: 'direct',
    messages: [
      {
        id: 'msg_e1',
        senderId: 'user_3',
        text: 'Are we launching the verified volunteers check system tonight?',
        timestamp: 'Yesterday',
        status: 'read'
      }
    ]
  }
];

export const INITIAL_NEARBY_POSTS: NearbyPost[] = [
  {
    id: 'post_1',
    user: MOCK_USERS[0], // Dr. Sophia
    content: '🚑 Medical camp set up at Metro Terminal 3. Free first-aid, hydration, and pediatric supplies available till 8 PM. Spread the word!',
    timestamp: '20m ago',
    likes: 34,
    comments: 8,
    hasLiked: false,
    distance: '400m away',
    tag: 'Medical Support'
  },
  {
    id: 'post_2',
    user: MOCK_USERS[2], // Elena
    content: '⚡ Power restored in East Side Sector 4. Thank you volunteer engineers for replacing the primary regulator box in record hours!',
    timestamp: '1h ago',
    likes: 89,
    comments: 12,
    hasLiked: true,
    distance: '800m away',
    tag: 'Infrastructure'
  },
  {
    id: 'post_3',
    user: MOCK_USERS[1], // Marcus
    content: '🍲 Setting up block kitchen at Community Center Room 2A. We received 5 bags of grain. Any extra volunteers with utensils are requested to arrive by 2 PM.',
    timestamp: '2h ago',
    likes: 45,
    comments: 15,
    hasLiked: false,
    distance: '1.2km away',
    tag: 'Community Kitchen'
  }
];

export const INITIAL_HELP_REQUESTS: HelpRequest[] = [
  {
    id: 'help_1',
    title: 'Urgent Insulin Vials Needed',
    description: 'Elderly patient requires rapid-acting insulin (Novorapid/Humalog) immediately. Power outage turned off refrigerator. Standard chemist is shut.',
    category: 'medical',
    urgency: 'critical',
    location: 'Apt 4B, Pinecrest Plaza',
    postedBy: {
      id: 'req_1_user',
      name: 'Rohan Sharma',
      username: '@rohan_sharma',
      avatar: 'RS',
      avatarBg: 'bg-orange-500',
      status: 'online',
      customStatus: '🏥 Hospital contact',
      bio: 'Resident coordinator at Pinecrest',
      role: 'member'
    },
    timestamp: '5m ago',
    verifiedBy: 'Red Cross Medical Volunteer Coordinator',
    status: 'unresolved',
    respondersCount: 2
  },
  {
    id: 'help_2',
    title: 'Water Supply Tripped (Elderly Home)',
    description: 'Fresh drinking water pump is down due to a burnt breaker coil at the Senior Living facility. Requesting temporary portable containers or a plumber.',
    category: 'utility',
    urgency: 'high',
    location: 'St. Jude Elder Care Hub',
    postedBy: {
      id: 'req_2_user',
      name: 'Sister Clara',
      username: '@st_jude_care',
      avatar: 'SC',
      avatarBg: 'bg-emerald-600',
      status: 'idle',
      customStatus: '🏡 On Duty',
      bio: 'Caretaker at St. Jude Retirement Home',
      role: 'member'
    },
    timestamp: '18m ago',
    verifiedBy: 'Municipal Utility Desk',
    status: 'investigating',
    respondersCount: 4
  },
  {
    id: 'help_3',
    title: 'Warm Food & Formula for Dry Shelter',
    description: 'We have 25 displaced families with babies in the Sector 6 school gym. Need baby milk powder, warm water flasks and simple biscuits.',
    category: 'food',
    urgency: 'high',
    location: 'Sector 6 Gym Shelter',
    postedBy: {
      id: 'req_3_user',
      name: 'Supervisor Jenkins',
      username: '@sector6_shelter',
      avatar: 'SJ',
      avatarBg: 'bg-purple-500',
      status: 'online',
      customStatus: '🏫 School Gym Coordinator',
      bio: 'Public community manager',
      role: 'member'
    },
    timestamp: '45m ago',
    verifiedBy: null,
    status: 'unresolved',
    respondersCount: 0
  }
];
