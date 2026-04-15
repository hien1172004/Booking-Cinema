package org.example.cinemaBooking.Shared.constant;


public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    public static final class Auth {
        public static final String BASE = "/auth";
        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String LOGOUT = "/logout";
        public static final String REFRESH = "/refresh";
        public static final String INTROSPECT = "/introspect";
        public static final String FORGOT_PASSWORD = "/forgot-password";
        public static final String RESET_PASSWORD = "/reset-password";
    }

    public static final class User {
        public static final String BASE = "/users";
        public static final String ME = "/me";
        public static final String CHANGE_PASSWORD = "/change-password";
        public static final String CHANGE_AVATAR = "/change-avatar" ;
        public static final String LOCK = "/lock";
        public static final String UNLOCK = "/unlock";
        public static final String STAFF = "/staff";
    }
    public static final class Movie {
        public static final String BASE = "/movies";
        public static final String NOW_SHOWING = "/now-showing";
        public static final String COMING_SOON = "/coming-soon";
        public static final String SEARCH = "/search";
        public static final String RECOMMENDED = "/recommended";
        public static final String IMAGE = "/images";
    }
    public static final class Cinema {
        public static final String BASE = "/cinema";
    }
    public static final class Room {
        public static final String BASE = "/rooms";
    }
    public static final class Seat {
        public static final String BASE = "/seats";
        public static final String SEAT_TYPE = "/seat_type";
    }
    public static final class Review {
        public static final String BASE = "/reviews";
        public static final String AVERAGE_RATING = "/average-rating";
    }
    public static final class Category {
        public static final String BASE = "/categories";
    }
    public static final class Product {
        public static final String BASE = "/products";
    }
    public static final class Combo {
        public static final String BASE = "/combos";
    }
    public static final class People {
         public static final String BASE = "/people";
    }
    public static final class Promotion {
        public static final String BASE = "/promotions";
    }
    public static final class Showtime {
        public static final String BASE = "/showtimes";
    }
    public static final class Booking {
        public static final String BASE = "/bookings";
    }
    public static final class Payment {
        public static final String BASE = "/payments";
    }
    public static final class Notification {
        public static final String BASE = "/notifications";
    }
    public static final class Ticket {
        public static final String BASE = "/tickets";
    }
    public static final class SeatType {
        public static final String BASE = "/seat-types";
    }
    public static final class Dashboard {
         public static final String BASE = "/dashboard";
         public static final String SUMMARY = "/summary";
         public static final String REVENUE_CHART = "/revenue-chart";
    }
    public static final class Statistic {
         public static final String BASE = "/statistics";
         public static final String SUMMARY = "/summary";
         public static final String TOP_MOVIES = "/top-movies";
         public static final String REVENUE_CHART = "/revenue-chart";
         public static final String TICKET_CHART = "/ticket-chart";
    }

    public static final class Cloudinary {
         public static final String BASE = "/cloudinary";
         public static final String UPLOAD_IMAGE = "/upload-image";
         public static final String UPLOAD_VIDEO = "/upload-video";
         public static final String DELETE = "/delete";
    }

    public static final class Chatbot {
        public static final String BASE = "/chatbot";
        public static final String CHAT = "/chat";
    }

}
