package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.entity.Order;

public record OrderCheckedOutEvent(Order order, String userEmail) {}
