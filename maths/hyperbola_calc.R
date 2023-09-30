# R routine - Calculate hyperbola
# @author timmyedge
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

## ATTEMPT to find t @ given point X,Y (i.e. assets location): 6,6, for given ddoa (to focus).
##      need to get a though, distance to vertex
##      is it possible to say @t=0: this corresponds to the vertex point, then equate what a is?
##    NOTE: distance between two foci is 2c. Thus c= ddoa / 2
X <- 6
Y <- 6
ddoa <- 1.1
c <- ddoa / 2
# acosh ~= ln(x + sqrt(x^2 - 1))

## Attempting to solve a=sqrt(c2 - b2), acosh(X/a) == asihn(Y/b)
## WOLFRAM INPUT: solve (X / (sqrt(c^2-b^2))) = (Y/b + sqrt(1+Y^2/b^2) + 1/(Y/b + sqrt(1+Y^2/b^2))) / 2 for b
##  THIS is not a bad resource: https://mathworld.wolfram.com/Hyperbola.html
b <- sqrt(sqrt((-c^2 + X^2 + Y^2)^2 + 4*c^2*Y^2 ) + c^2-X^2-Y^2) / sqrt(2)
a <- sqrt(c^2 - b^2)
print (paste('X: ',X,', Y: ',Y,', c: ',c,', b: ',b,', a: ',a))

xv <- a * cosh(0)
yv <- b * sinh(0)
print (paste('Xv: ',xv,', Yv: ',yv))