-- phpMyAdmin SQL Dump
-- version 4.2.10
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Jan 31, 2020 at 12:09 AM
-- Server version: 5.5.38
-- PHP Version: 5.6.2

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `directmappingtest`
--

-- --------------------------------------------------------

--
-- Table structure for table `likes`
--

CREATE TABLE `likes` (
  `person_id` int(11) NOT NULL,
  `angle1` int(11) NOT NULL,
  `angle2` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `likes`
--

INSERT INTO `likes` (`person_id`, `angle1`, `angle2`) VALUES
(1, 60, 60),
(1, 70, 70),
(2, 70, 70);

-- --------------------------------------------------------

--
-- Table structure for table `person`
--

CREATE TABLE `person` (
`id` int(11) NOT NULL,
  `fname` varchar(25) NOT NULL,
  `lname` varchar(25) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `person`
--

INSERT INTO `person` (`id`, `fname`, `lname`) VALUES
(1, 'Jane', 'Doe'),
(2, 'John', 'Doe');

-- --------------------------------------------------------

--
-- Table structure for table `triangle`
--

CREATE TABLE `triangle` (
  `angle1` int(3) NOT NULL,
  `angle2` int(3) NOT NULL,
  `angle3` int(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `triangle`
--

INSERT INTO `triangle` (`angle1`, `angle2`, `angle3`) VALUES
(60, 60, 60),
(70, 70, 20);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `likes`
--
ALTER TABLE `likes`
 ADD PRIMARY KEY (`person_id`,`angle1`,`angle2`), ADD KEY `angle1` (`angle1`,`angle2`);

--
-- Indexes for table `person`
--
ALTER TABLE `person`
 ADD PRIMARY KEY (`id`);

--
-- Indexes for table `triangle`
--
ALTER TABLE `triangle`
 ADD PRIMARY KEY (`angle1`,`angle2`), ADD UNIQUE KEY `angle2` (`angle2`,`angle3`), ADD UNIQUE KEY `angle1` (`angle1`,`angle3`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `person`
--
ALTER TABLE `person`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=3;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `likes`
--
ALTER TABLE `likes`
ADD CONSTRAINT `likes_ibfk_2` FOREIGN KEY (`angle1`, `angle2`) REFERENCES `triangle` (`angle1`, `angle2`),
ADD CONSTRAINT `likes_ibfk_1` FOREIGN KEY (`person_id`) REFERENCES `person` (`id`);
