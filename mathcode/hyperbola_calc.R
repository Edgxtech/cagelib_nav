 # double[] r_a_utm = Helpers.convertLatLngToUtmNthingEasting(r_a.getLat(), r_a.getLon()); // RETURNS IN [NTHING===Y , EASTING===X]
 # double[] r_b_utm = Helpers.convertLatLngToUtmNthingEasting(r_b.getLat(), r_b.getLon());

 # double c = Math.sqrt(Math.pow((r_a_utm[1] - r_b_utm[1]), 2) + Math.pow((r_a_utm[0] - r_b_utm[0]), 2)) / 2; // focus length from origin, +-c respectively
 # double a = (obs.getMeas() * Helpers.SPEED_OF_LIGHT) / 2;
 # double b = Math.sqrt(Math.abs(Math.pow(c, 2) - Math.pow(a, 2))); // c = sqrt(a^2+b^2)
 # double ca = (r_b_utm[1] - r_a_utm[1]) / (2 * c);
 # double sa = (r_b_utm[0] - r_a_utm[0]) / (2 * c); //# COS and SIN of rot angle
 # for (double t = -2; t <= 2; t += 0.1) {
 	# double X = a * Math.cosh(t);
 	# double Y = b * Math.sinh(t); //# Hyperbola branch
 	# double x = (r_a_utm[1] + r_b_utm[1]) / 2 + X * ca - Y * sa; //# Rotated and translated
    # double y = (r_a_utm[0] + r_b_utm[0]) / 2 + X * sa + Y * ca;
    # UTMRef utmMeas = new UTMRef(x, y, this.geoMission.getLatZone(), this.geoMission.getLonZone());
    # LatLng ltln = utmMeas.toLatLng();
    # measurementHyperbola.add(new double[]{ltln.getLat(), ltln.getLng()});
  # }
                    
c <- 2^2
print(c)

x1 <- 5
y1 <- 5
x2 <- 10
y2 <- 10
ddoa <- 500 ## ddoa = tdoa * speed_of_light

c = sqrt((x1 - x2)^2 + (y1 - y2)^2) / 2; # focus length from origin, +-c respectively
a <- (ddoa) / 2;
b <- sqrt(abs(c^2 - a^2)); # c = sqrt(a^2+b^2)
ca <- (x2 - x1) / (2 * c); 
sa <- (y2 - y1) / (2 * c); # COS and SIN of rot angle

print(c)
print(a)
print(b)
print(ca)
print(sa)

result <- list()

   X <- a * cosh(0);
   Y <- b * sinh(0); # Hyperbola branch
   x <- (x1 + x2) / 2 + X * ca - Y * sa; # Rotated and translated
   y <- (y1 + y2) / 2 + X * sa + Y * ca;
x_y <- data.frame(x,y)
colnames(x_y) <- c("x","y")

for(t in seq(-2, 2, 0.1)) {
	print(t)
   X <- a * cosh(t);
   Y <- b * sinh(t); # Hyperbola branch
   x <- (x1 + x2) / 2 + X * ca - Y * sa; # Rotated and translated
   y <- (y1 + y2) / 2 + X * sa + Y * ca;
   
	x_y_ <- data.frame(x,y)
	colnames(x_y_) <- c("x","y")
	
   x_y <- rbind(x_y, x_y_)
}

print(x_y)
plot(x_y$x,x_y$y) ## PLOTS Single hyperbola branch


