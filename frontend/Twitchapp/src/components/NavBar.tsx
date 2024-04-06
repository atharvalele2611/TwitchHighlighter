import {
  Button,
  Flex,
  HStack,
  Image,
  Input,
  InputGroup,
  InputLeftElement,
  Link,
  Center,
  SimpleGrid
} from "@chakra-ui/react";
import React, { useEffect, useRef, useState } from "react";
import logo from "../assets/Twitch_TV.png";
import { AiFillHome } from "react-icons/ai";
import { BsSearch } from "react-icons/bs";
import ChannelCard from "./ChannelCard";
import { BrowserRouter, useNavigate, NavLink as RouteLink } from "react-router-dom";
import { searchChannel } from "../services/api-client";
import { Channel } from "./ChannelGrid";

interface Props {
  onSearch: (text: string) => void;
}

const NavBar = ({ onSearch }: Props) => {
  const [channels, setchannel] = useState<Channel[]>([]);
  const [searchText, setSearchText] = useState("");
  const ref = useRef<HTMLInputElement>(null);
  useEffect(() => {
    // console.log(searchText);
  }, [searchText]);

  const handleSearch = () => {
    const text = ref.current?.value ?? "";
    setSearchText(text);
    onSearch(text);
  };
  const navigate = useNavigate();
  const navigateToHome = () => {
    navigate("/");
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSearch(searchText);
    navigate("/search");
  };
  
  return (
    <HStack>
      {/* <Image src={logo} boxSize="55px" /> */}
        <Image src={logo} boxSize="55px" onClick={navigateToHome} />  
        <form onSubmit={handleSubmit}>
          <Flex>
            <InputGroup>
              <InputLeftElement children={<BsSearch />} />
              <Input
                ref={ref}
                borderRadius={20}
                placeholder="Search twitch channels....."
                variant={"filled"}
                value={searchText}
                onChange={handleSearch}
              ></Input>
              <Button type="submit" colorScheme="teal">
                  Search
                </Button>
              </InputGroup>
            </Flex>
        </form>
    </HStack>
  );
};

export default NavBar;
